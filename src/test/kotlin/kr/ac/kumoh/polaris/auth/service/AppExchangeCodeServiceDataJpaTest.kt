package kr.ac.kumoh.polaris.auth.service

import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.auth.config.JwtProperties
import kr.ac.kumoh.polaris.auth.entity.AppExchangeCodeStatus
import kr.ac.kumoh.polaris.auth.presentation.request.ExchangeCodeRequest
import kr.ac.kumoh.polaris.auth.repository.AppExchangeCodeRepository
import kr.ac.kumoh.polaris.auth.repository.RefreshTokenRepository
import kr.ac.kumoh.polaris.auth.util.JwtTokenProvider
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Base64

@DataJpaTest
@Import(
    AuthTokenService::class,
    AppExchangeCodeService::class,
    JwtTokenProvider::class,
    AppExchangeCodeServiceDataJpaTest.TestConfig::class
)
class AppExchangeCodeServiceDataJpaTest(
    @Autowired private val appExchangeCodeService: AppExchangeCodeService,
    @Autowired private val authTokenService: AuthTokenService,
    @Autowired private val appExchangeCodeRepository: AppExchangeCodeRepository,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository,
    @Autowired private val userRepository: UserRepository
) {
    @Test
    fun `exchange redeems proof bound code once and records issued state`() {
        val user = userRepository.save(testUser("proof-success"))
        val verifier = verifier("proof-success")
        val exchangeCode = appExchangeCodeService.issueCode(user, "mobile-app", challenge(verifier))

        val response = appExchangeCodeService.exchange(
            ExchangeCodeRequest(
                code = exchangeCode,
                targetId = "mobile-app",
                codeVerifier = verifier
            )
        )

        assertEquals(user.id, response.userId)
        assertNotNull(refreshTokenRepository.findByToken(response.refreshToken))
        val persisted = appExchangeCodeRepository.findByCodeHash(hash(exchangeCode))
        assertEquals(AppExchangeCodeStatus.ISSUED, persisted?.issuanceStatus)
        assertNotNull(persisted?.issuedRefreshTokenId)
    }

    @Test
    fun `exchange requires verifier for proof bound code`() {
        val user = userRepository.save(testUser("proof-required"))
        val exchangeCode = appExchangeCodeService.issueCode(user, "mobile-app", challenge(verifier("proof-required")))

        val exception = assertThrows(ServiceException::class.java) {
            appExchangeCodeService.exchange(
                ExchangeCodeRequest(
                    code = exchangeCode,
                    targetId = "mobile-app",
                    codeVerifier = null
                )
            )
        }

        assertEquals(ErrorCode.OIDC_PROOF_REQUIRED, exception.errorCode)
    }

    @Test
    fun `exchange rejects mismatched verifier for proof bound code`() {
        val user = userRepository.save(testUser("proof-mismatch"))
        val exchangeCode = appExchangeCodeService.issueCode(user, "mobile-app", challenge(verifier("proof-mismatch")))

        val exception = assertThrows(ServiceException::class.java) {
            appExchangeCodeService.exchange(
                ExchangeCodeRequest(
                    code = exchangeCode,
                    targetId = "mobile-app",
                    codeVerifier = verifier("different")
                )
            )
        }

        assertEquals(ErrorCode.OIDC_PROOF_MISMATCH, exception.errorCode)
    }

    @Test
    fun `exchange allows proofless codes only when compatibility mode is explicitly enabled`() {
        val compatibilityService = AppExchangeCodeService(
            authTokenService = authTokenService,
            appExchangeCodeRepository = appExchangeCodeRepository,
            refreshTokenRepository = refreshTokenRepository,
            authAppLoginProperties = AuthAppLoginProperties(ttlSeconds = 180, allowWithoutProof = true)
        )
        val user = userRepository.save(testUser("proofless"))
        val exchangeCode = compatibilityService.issueCode(user, "mobile-app", null)

        val response = compatibilityService.exchange(
            ExchangeCodeRequest(
                code = exchangeCode,
                targetId = "mobile-app",
                codeVerifier = null
            )
        )

        assertEquals(user.id, response.userId)
    }

    @Test
    fun `exchange rejects duplicate redemption after successful issuance`() {
        val user = userRepository.save(testUser("already-consumed"))
        val verifier = verifier("already-consumed")
        val exchangeCode = appExchangeCodeService.issueCode(user, "mobile-app", challenge(verifier))

        appExchangeCodeService.exchange(
            ExchangeCodeRequest(
                code = exchangeCode,
                targetId = "mobile-app",
                codeVerifier = verifier
            )
        )

        val exception = assertThrows(ServiceException::class.java) {
            appExchangeCodeService.exchange(
                ExchangeCodeRequest(
                    code = exchangeCode,
                    targetId = "mobile-app",
                    codeVerifier = verifier
                )
            )
        }

        assertEquals(ErrorCode.OIDC_EXCHANGE_CODE_ALREADY_CONSUMED, exception.errorCode)
    }

    @Test
    fun `exchange rejects expired code`() {
        val user = userRepository.save(testUser("expired"))
        val verifier = verifier("expired")
        val exchangeCode = appExchangeCodeService.issueCode(
            user = user,
            targetId = "mobile-app",
            codeChallenge = challenge(verifier),
            expiresAt = LocalDateTime.now().minusSeconds(1)
        )

        val exception = assertThrows(ServiceException::class.java) {
            appExchangeCodeService.exchange(
                ExchangeCodeRequest(
                    code = exchangeCode,
                    targetId = "mobile-app",
                    codeVerifier = verifier
                )
            )
        }

        assertEquals(ErrorCode.OIDC_EXCHANGE_CODE_EXPIRED, exception.errorCode)
    }

    @Test
    fun `issue code rejects proofless mode by default`() {
        val user = userRepository.save(testUser("proof-default-off"))

        val exception = assertThrows(ServiceException::class.java) {
            appExchangeCodeService.issueCode(user, "mobile-app", null)
        }

        assertEquals(ErrorCode.OIDC_PROOF_REQUIRED, exception.errorCode)
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun jwtProperties(): JwtProperties = JwtProperties(
            issuer = "https://polaris.test",
            secret = "polaris-auth-jwt-secret-key-polstar-2026-32bytes",
            accessTokenExpirationSeconds = 3600,
            refreshTokenExpirationSeconds = 1209600
        )

        @Bean
        fun authAppLoginProperties(): AuthAppLoginProperties = AuthAppLoginProperties(
            ttlSeconds = 180,
            allowWithoutProof = false
        )
    }

    private fun testUser(suffix: String): User = User(
        provider = UserAuthProvider.KAKAO,
        oidcIssuer = "https://kauth.kakao.com",
        oidcSubject = "subject-$suffix",
        nickname = "tester-$suffix",
        email = "tester-$suffix@example.com"
    )

    private fun verifier(seed: String): String = (seed + "-verifier-abcdefghijklmnopqrstuvwxyz-0123456789-ABCDE").take(64)

    private fun challenge(verifier: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.US_ASCII)))

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.US_ASCII))
        .joinToString(separator = "") { "%02x".format(it) }
}
