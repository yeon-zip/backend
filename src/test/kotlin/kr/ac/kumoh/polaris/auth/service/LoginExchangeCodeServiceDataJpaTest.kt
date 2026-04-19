package kr.ac.kumoh.polaris.auth.service

import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.auth.config.JwtProperties
import kr.ac.kumoh.polaris.auth.entity.LoginExchangeCodeStatus
import kr.ac.kumoh.polaris.auth.repository.LoginExchangeCodeRepository
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
import org.springframework.transaction.support.TransactionTemplate
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64

@DataJpaTest
@Import(
    AuthTokenService::class,
    JwtTokenProvider::class,
    LoginExchangeCodeService::class,
    LoginExchangeCodeServiceDataJpaTest.TestConfig::class
)
class LoginExchangeCodeServiceDataJpaTest(
    @Autowired private val loginExchangeCodeService: LoginExchangeCodeService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val loginExchangeCodeRepository: LoginExchangeCodeRepository,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository,
    @Autowired private val authTokenService: AuthTokenService,
    @Autowired private val transactionTemplate: TransactionTemplate
) {
    @Test
    fun `proof-bound exchange code redeems exactly once`() {
        val user = userRepository.save(testUser("proof-bound"))
        val codeVerifier = validCodeVerifier("proof-bound")
        val codeChallenge = pkceS256(codeVerifier)

        val exchangeCode = loginExchangeCodeService.issueExchangeCode(
            user = user,
            targetId = "mobile",
            codeChallenge = codeChallenge,
            correlationId = "corr-1"
        )

        val tokenResponse = loginExchangeCodeService.redeem(
            code = exchangeCode,
            targetId = "mobile",
            codeVerifier = codeVerifier
        )

        val persisted = loginExchangeCodeRepository.findByCodeHash(sha256Hex(exchangeCode))
        assertEquals(LoginExchangeCodeStatus.ISSUED, persisted?.issuanceStatus)
        assertNotNull(persisted?.issuedRefreshTokenId)
        assertEquals(user.id, tokenResponse.userId)
        assertNotNull(refreshTokenRepository.findByToken(tokenResponse.refreshToken))

        val duplicate = assertThrows(ServiceException::class.java) {
            loginExchangeCodeService.redeem(exchangeCode, "mobile", codeVerifier)
        }
        assertEquals(ErrorCode.OIDC_EXCHANGE_CODE_ALREADY_CONSUMED, duplicate.errorCode)
    }

    @Test
    fun `proof-bound exchange requires verifier and rejects mismatch`() {
        val user = userRepository.save(testUser("mismatch"))
        val codeVerifier = validCodeVerifier("mismatch")
        val exchangeCode = loginExchangeCodeService.issueExchangeCode(
            user = user,
            targetId = "mobile",
            codeChallenge = pkceS256(codeVerifier)
        )

        val missingVerifier = assertThrows(ServiceException::class.java) {
            loginExchangeCodeService.redeem(exchangeCode, "mobile", null)
        }
        assertEquals(ErrorCode.OIDC_PROOF_REQUIRED, missingVerifier.errorCode)

        val mismatch = assertThrows(ServiceException::class.java) {
            loginExchangeCodeService.redeem(exchangeCode, "mobile", validCodeVerifier("different"))
        }
        assertEquals(ErrorCode.OIDC_PROOF_MISMATCH, mismatch.errorCode)
    }

    @Test
    fun `proof-bound exchange rejects syntactically invalid verifier`() {
        val user = userRepository.save(testUser("invalid-verifier"))
        val codeVerifier = validCodeVerifier("invalid-verifier")
        val exchangeCode = loginExchangeCodeService.issueExchangeCode(
            user = user,
            targetId = "mobile",
            codeChallenge = pkceS256(codeVerifier)
        )

        val exception = assertThrows(ServiceException::class.java) {
            loginExchangeCodeService.redeem(exchangeCode, "mobile", "short")
        }
        assertEquals(ErrorCode.OIDC_INVALID_CODE_VERIFIER, exception.errorCode)
    }

    @Test
    fun `proofless exchange works only when compatibility mode is enabled`() {
        val compatibilityService = LoginExchangeCodeService(
            authAppLoginProperties = AuthAppLoginProperties(
                app = AuthAppLoginProperties.App(
                    targets = mapOf(
                        "mobile" to "polaris://auth/callback",
                        "tablet" to "https://app.polaris.test/auth/callback"
                    ),
                    exchange = AuthAppLoginProperties.Exchange(
                        ttl = Duration.ofMinutes(3),
                        allowWithoutProof = true
                    )
                )
            ),
            loginExchangeCodeRepository = loginExchangeCodeRepository,
            authTokenService = authTokenService,
            transactionTemplate = transactionTemplate
        )
        val user = userRepository.save(testUser("compatibility"))

        val exchangeCode = compatibilityService.issueExchangeCode(
            user = user,
            targetId = "mobile",
            codeChallenge = null
        )

        val tokenResponse = compatibilityService.redeem(
            code = exchangeCode,
            targetId = "mobile",
            codeVerifier = null
        )

        val persisted = loginExchangeCodeRepository.findByCodeHash(sha256Hex(exchangeCode))
        assertEquals(LoginExchangeCodeStatus.ISSUED, persisted?.issuanceStatus)
        assertEquals(user.id, tokenResponse.userId)
        assertNotNull(refreshTokenRepository.findByToken(tokenResponse.refreshToken))
    }

    @Test
    fun `issuing proofless exchange code is disabled by default`() {
        val user = userRepository.save(testUser("proofless-default-off"))

        val exception = assertThrows(ServiceException::class.java) {
            loginExchangeCodeService.issueExchangeCode(user, "mobile", null)
        }
        assertEquals(ErrorCode.OIDC_PROOF_REQUIRED, exception.errorCode)
    }

    @Test
    fun `bound target mismatch is rejected during redemption`() {
        val user = userRepository.save(testUser("wrong-target"))
        val codeVerifier = validCodeVerifier("wrong-target")
        val exchangeCode = loginExchangeCodeService.issueExchangeCode(
            user = user,
            targetId = "mobile",
            codeChallenge = pkceS256(codeVerifier)
        )

        val exception = assertThrows(ServiceException::class.java) {
            loginExchangeCodeService.redeem(exchangeCode, "tablet", codeVerifier)
        }
        assertEquals(ErrorCode.OIDC_EXCHANGE_TARGET_MISMATCH, exception.errorCode)
    }

    @Test
    fun `unknown target is rejected before redemption`() {
        val user = userRepository.save(testUser("unknown-target"))
        val codeVerifier = validCodeVerifier("unknown-target")
        val exchangeCode = loginExchangeCodeService.issueExchangeCode(
            user = user,
            targetId = "mobile",
            codeChallenge = pkceS256(codeVerifier)
        )

        val exception = assertThrows(ServiceException::class.java) {
            loginExchangeCodeService.redeem(exchangeCode, "desktop", codeVerifier)
        }
        assertEquals(ErrorCode.OIDC_TARGET_NOT_ALLOWED, exception.errorCode)
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
            app = AuthAppLoginProperties.App(
                targets = mapOf(
                    "mobile" to "polaris://auth/callback",
                    "tablet" to "https://app.polaris.test/auth/callback"
                ),
                exchange = AuthAppLoginProperties.Exchange(
                    ttl = Duration.ofMinutes(3),
                    allowWithoutProof = false
                )
            )
        )
    }

    private fun testUser(suffix: String): User = User(
        provider = UserAuthProvider.KAKAO,
        oidcIssuer = "https://kauth.kakao.com",
        oidcSubject = "subject-$suffix",
        nickname = "tester-$suffix",
        email = "tester-$suffix@example.com"
    )

    private fun validCodeVerifier(seed: String): String = (seed + "-verifier-").repeat(4).take(43)

    private fun pkceS256(codeVerifier: String): String {
        val hashed = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed)
    }

    private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString(separator = "") { "%02x".format(it) }
}
