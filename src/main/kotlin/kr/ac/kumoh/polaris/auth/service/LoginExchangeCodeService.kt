package kr.ac.kumoh.polaris.auth.service

import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.auth.entity.LoginExchangeCode
import kr.ac.kumoh.polaris.auth.entity.LoginExchangeCodeStatus
import kr.ac.kumoh.polaris.auth.presentation.response.AuthTokenResponse
import kr.ac.kumoh.polaris.auth.repository.LoginExchangeCodeRepository
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64

@Service
class LoginExchangeCodeService(
    private val authAppLoginProperties: AuthAppLoginProperties,
    private val loginExchangeCodeRepository: LoginExchangeCodeRepository,
    private val authTokenService: AuthTokenService,
    private val transactionTemplate: TransactionTemplate
) {
    fun issueExchangeCode(
        user: User,
        targetId: String,
        codeChallenge: String?,
        correlationId: String? = null
    ): String {
        validateTarget(targetId)
        validateCodeChallenge(codeChallenge)

        val rawCode = generateExchangeCode()
        val exchangeCode = LoginExchangeCode(
            codeHash = sha256Hex(rawCode),
            targetId = targetId,
            user = user,
            expiresAt = LocalDateTime.now().plus(authAppLoginProperties.app.exchange.ttl),
            codeChallengeHash = codeChallenge?.let(::sha256Hex),
            correlationId = correlationId
        )
        loginExchangeCodeRepository.save(exchangeCode)
        return rawCode
    }

    fun redeem(
        code: String,
        targetId: String,
        codeVerifier: String?
    ): AuthTokenResponse {
        validateTarget(targetId)

        val result = transactionTemplate.execute {
            val exchangeCode = loginExchangeCodeRepository.findByCodeHash(sha256Hex(code))
                ?: return@execute RedemptionResult.Failure(ServiceException(ErrorCode.OIDC_EXCHANGE_CODE_INVALID))

            validateExchangeCode(exchangeCode, targetId, codeVerifier)

            try {
                val issuedTokens = authTokenService.issueTokensWithRefreshToken(exchangeCode.user)
                val refreshTokenId = issuedTokens.refreshTokenId
                    ?: throw ServiceException(ErrorCode.OIDC_TERMINAL_ISSUANCE_FAILURE)
                exchangeCode.markIssued(refreshTokenId)
                RedemptionResult.Success(issuedTokens.response)
            } catch (exception: RuntimeException) {
                exchangeCode.markFailedTerminal(exception.message ?: ErrorCode.OIDC_TERMINAL_ISSUANCE_FAILURE.name)
                RedemptionResult.Failure(asIssuanceFailure(exception))
            }
        } ?: throw ServiceException(ErrorCode.OIDC_TERMINAL_ISSUANCE_FAILURE)

        return when (result) {
            is RedemptionResult.Success -> result.response
            is RedemptionResult.Failure -> throw result.exception
        }
    }

    private fun validateExchangeCode(
        exchangeCode: LoginExchangeCode,
        targetId: String,
        codeVerifier: String?
    ) {
        if (exchangeCode.issuanceStatus == LoginExchangeCodeStatus.ISSUED) {
            throw ServiceException(ErrorCode.OIDC_EXCHANGE_CODE_ALREADY_CONSUMED)
        }
        if (exchangeCode.issuanceStatus == LoginExchangeCodeStatus.FAILED_TERMINAL) {
            throw ServiceException(ErrorCode.OIDC_TERMINAL_ISSUANCE_FAILURE)
        }
        if (exchangeCode.isExpired()) {
            throw ServiceException(ErrorCode.OIDC_EXCHANGE_CODE_EXPIRED)
        }
        if (exchangeCode.targetId != targetId) {
            throw ServiceException(ErrorCode.OIDC_EXCHANGE_TARGET_MISMATCH)
        }

        val storedChallengeHash = exchangeCode.codeChallengeHash ?: return
        val normalizedVerifier = codeVerifier?.trim().takeUnless { it.isNullOrEmpty() }
            ?: throw ServiceException(ErrorCode.OIDC_PROOF_REQUIRED)
        validateCodeVerifier(normalizedVerifier)
        val derivedChallenge = pkceS256(normalizedVerifier)
        if (sha256Hex(derivedChallenge) != storedChallengeHash) {
            throw ServiceException(ErrorCode.OIDC_PROOF_MISMATCH)
        }
    }

    private fun validateTarget(targetId: String) {
        if (authAppLoginProperties.resolveTarget(targetId).isNullOrBlank()) {
            throw ServiceException(ErrorCode.OIDC_TARGET_NOT_ALLOWED)
        }
    }

    private fun validateCodeChallenge(codeChallenge: String?) {
        if (codeChallenge.isNullOrBlank() && !authAppLoginProperties.app.exchange.allowWithoutProof) {
            throw ServiceException(ErrorCode.OIDC_PROOF_REQUIRED)
        }
    }

    private fun validateCodeVerifier(codeVerifier: String) {
        if (!CODE_VERIFIER_PATTERN.matches(codeVerifier)) {
            throw ServiceException(ErrorCode.OIDC_INVALID_CODE_VERIFIER)
        }
    }

    private fun asIssuanceFailure(exception: RuntimeException): RuntimeException = when (exception) {
        is ServiceException -> ServiceException(ErrorCode.OIDC_TERMINAL_ISSUANCE_FAILURE, exception.message)
        else -> ServiceException(ErrorCode.OIDC_TERMINAL_ISSUANCE_FAILURE)
    }

    private fun generateExchangeCode(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun pkceS256(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed)
    }

    private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.US_ASCII))
        .joinToString(separator = "") { "%02x".format(it) }

    private sealed interface RedemptionResult {
        data class Success(val response: AuthTokenResponse) : RedemptionResult
        data class Failure(val exception: RuntimeException) : RedemptionResult
    }

    companion object {
        private val secureRandom = SecureRandom()
        private val CODE_VERIFIER_PATTERN = Regex("^[A-Za-z0-9._~-]{43,128}$")
    }
}
