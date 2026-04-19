package kr.ac.kumoh.polaris.auth.service

import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.auth.entity.AppExchangeCode
import kr.ac.kumoh.polaris.auth.entity.AppExchangeCodeStatus
import kr.ac.kumoh.polaris.auth.presentation.request.ExchangeCodeRequest
import kr.ac.kumoh.polaris.auth.presentation.response.AuthTokenResponse
import kr.ac.kumoh.polaris.auth.repository.AppExchangeCodeRepository
import kr.ac.kumoh.polaris.auth.repository.RefreshTokenRepository
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID

@Service
@Transactional
class AppExchangeCodeService(
    private val authTokenService: AuthTokenService,
    private val appExchangeCodeRepository: AppExchangeCodeRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val authAppLoginProperties: AuthAppLoginProperties
) {
    fun issueCode(
        user: User,
        targetId: String,
        codeChallenge: String?,
        expiresAt: LocalDateTime = LocalDateTime.now().plusSeconds(authAppLoginProperties.ttlSeconds)
    ): String {
        require(targetId.isNotBlank()) { "targetId must not be blank" }

        if (codeChallenge == null && !authAppLoginProperties.allowWithoutProof) {
            throw ServiceException(ErrorCode.OIDC_PROOF_REQUIRED)
        }

        val rawCode = UUID.randomUUID().toString().replace("-", "")
        val exchangeCode = AppExchangeCode(
            codeHash = sha256Hex(rawCode),
            targetId = targetId,
            codeChallengeHash = codeChallenge?.let(::sha256Hex),
            expiresAt = expiresAt,
            user = user
        )
        appExchangeCodeRepository.save(exchangeCode)
        return rawCode
    }

    fun exchange(request: ExchangeCodeRequest): AuthTokenResponse {
        val exchangeCode = appExchangeCodeRepository.findByCodeHashForUpdate(sha256Hex(request.code))
            ?: throw ServiceException(ErrorCode.OIDC_EXCHANGE_CODE_INVALID)

        if (exchangeCode.issuanceStatus != AppExchangeCodeStatus.PENDING || exchangeCode.consumedAt != null) {
            throw ServiceException(ErrorCode.OIDC_EXCHANGE_CODE_ALREADY_CONSUMED)
        }

        if (exchangeCode.isExpired()) {
            throw ServiceException(ErrorCode.OIDC_EXCHANGE_CODE_EXPIRED)
        }

        if (exchangeCode.targetId != request.targetId) {
            throw ServiceException(ErrorCode.OIDC_EXCHANGE_TARGET_MISMATCH)
        }

        validateProofBinding(exchangeCode, request.codeVerifier)

        val tokenResponse = authTokenService.issueTokens(exchangeCode.user)
        val refreshTokenId = refreshTokenRepository.findByToken(tokenResponse.refreshToken)?.id
        exchangeCode.markIssued(refreshTokenId)

        return tokenResponse
    }

    private fun validateProofBinding(exchangeCode: AppExchangeCode, codeVerifier: String?) {
        val storedChallengeHash = exchangeCode.codeChallengeHash
        if (storedChallengeHash == null) {
            return
        }

        val verifier = codeVerifier ?: throw ServiceException(ErrorCode.OIDC_PROOF_REQUIRED)
        if (!CODE_VERIFIER_PATTERN.matches(verifier) || verifier.length !in 43..128) {
            throw ServiceException(ErrorCode.OIDC_INVALID_CODE_VERIFIER)
        }

        val derivedChallenge = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(sha256Bytes(verifier))

        if (sha256Hex(derivedChallenge) != storedChallengeHash) {
            throw ServiceException(ErrorCode.OIDC_PROOF_MISMATCH)
        }
    }

    private fun sha256Hex(value: String): String = sha256Bytes(value).joinToString(separator = "") { "%02x".format(it) }

    private fun sha256Bytes(value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.US_ASCII))

    companion object {
        private val CODE_VERIFIER_PATTERN = Regex("^[A-Za-z0-9\-._~]+$")
    }
}
