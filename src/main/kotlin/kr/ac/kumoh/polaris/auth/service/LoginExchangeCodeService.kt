package kr.ac.kumoh.polaris.auth.service

import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.auth.entity.LoginExchangeCode
import kr.ac.kumoh.polaris.auth.entity.LoginExchangeIssuanceStatus
import kr.ac.kumoh.polaris.auth.presentation.request.ExchangeCodeRequest
import kr.ac.kumoh.polaris.auth.presentation.response.AuthTokenResponse
import kr.ac.kumoh.polaris.auth.repository.LoginExchangeCodeRepository
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Base64

@Service
class LoginExchangeCodeService(
    private val loginExchangeCodeRepository: LoginExchangeCodeRepository,
    private val authAppLoginProperties: AuthAppLoginProperties,
    private val authTokenService: AuthTokenService
) {
    @Transactional
    fun issueAppExchangeCode(
        user: User,
        targetId: String,
        codeChallenge: String?,
        correlationId: String
    ): String {
        val exchangeCode = generateOpaqueExchangeCode()
        loginExchangeCodeRepository.save(
            LoginExchangeCode(
                codeHash = sha256(exchangeCode),
                targetId = targetId,
                codeChallengeHash = codeChallenge?.let(::sha256),
                expiresAt = LocalDateTime.now().plusSeconds(authAppLoginProperties.exchangeCodeTtlSeconds),
                correlationId = correlationId,
                user = user
            )
        )
        return exchangeCode
    }

    @Transactional(noRollbackFor = [ServiceException::class])
    fun exchange(request: ExchangeCodeRequest): AuthTokenResponse {
        val now = LocalDateTime.now()
        val exchangeCode = loginExchangeCodeRepository.findByCodeHashForUpdate(sha256(request.code.trim()))
            ?: throw ServiceException(
                ErrorCode.OIDC_INVALID_EXCHANGE_CODE,
                "유효하지 않은 교환 코드입니다."
            )

        when (exchangeCode.issuanceStatus) {
            LoginExchangeIssuanceStatus.ISSUED -> throw ServiceException(
                ErrorCode.OIDC_EXCHANGE_CODE_ALREADY_CONSUMED,
                "이미 사용된 교환 코드입니다."
            )
            LoginExchangeIssuanceStatus.FAILED_TERMINAL -> throw ServiceException(
                ErrorCode.OIDC_EXCHANGE_TERMINAL_FAILURE,
                "더 이상 사용할 수 없는 교환 코드입니다."
            )
            LoginExchangeIssuanceStatus.PENDING -> Unit
        }

        if (exchangeCode.isExpired(now)) {
            return failTerminal(exchangeCode, now, ErrorCode.OIDC_EXCHANGE_CODE_EXPIRED, "EXPIRED")
        }

        if (exchangeCode.targetId != request.targetId.trim()) {
            return failTerminal(exchangeCode, now, ErrorCode.OIDC_INVALID_TARGET, "TARGET_MISMATCH")
        }

        validateProofBinding(exchangeCode, request.codeVerifier, now)

        val issuedTokens = authTokenService.issueTokensWithRefreshToken(exchangeCode.user)
        exchangeCode.markIssued(now, issuedTokens.refreshToken.id)
        return issuedTokens.response
    }

    private fun validateProofBinding(
        exchangeCode: LoginExchangeCode,
        codeVerifierValue: String?,
        now: LocalDateTime
    ) {
        val storedChallengeHash = exchangeCode.codeChallengeHash ?: return
        val codeVerifier = codeVerifierValue?.trim()?.takeIf { it.isNotEmpty() }
            ?: failTerminal(exchangeCode, now, ErrorCode.OIDC_PROOF_REQUIRED, "PROOF_REQUIRED")

        if (!CODE_VERIFIER_PATTERN.matches(codeVerifier)) {
            failTerminal(exchangeCode, now, ErrorCode.OIDC_INVALID_CODE_VERIFIER, "INVALID_CODE_VERIFIER")
        }

        val derivedChallenge = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII)))

        if (sha256(derivedChallenge) != storedChallengeHash) {
            failTerminal(exchangeCode, now, ErrorCode.OIDC_PROOF_MISMATCH, "PROOF_MISMATCH")
        }
    }

    private fun failTerminal(
        exchangeCode: LoginExchangeCode,
        now: LocalDateTime,
        errorCode: ErrorCode,
        reason: String
    ): Nothing {
        exchangeCode.markFailed(now, reason)
        throw ServiceException(errorCode, errorCode.message)
    }

    private fun generateOpaqueExchangeCode(): String {
        val bytes = ByteArray(32)
        SECURE_RANDOM.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        private val SECURE_RANDOM = java.security.SecureRandom()
        private val CODE_VERIFIER_PATTERN = Regex("^[A-Za-z0-9._~-]{43,128}$")
    }
}
