package kr.ac.kumoh.polaris.auth.service

import kr.ac.kumoh.polaris.auth.config.JwtProperties
import kr.ac.kumoh.polaris.auth.repository.RefreshTokenRepository
import kr.ac.kumoh.polaris.auth.util.JwtTokenProvider
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(AuthTokenService::class, JwtTokenProvider::class, AuthTokenServiceDataJpaTest.TestConfig::class)
class AuthTokenServiceDataJpaTest(
    @Autowired private val authTokenService: AuthTokenService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository
) {
    @Test
    fun `issue tokens saves refresh token`() {
        val user = userRepository.save(testUser("issue"))

        val tokenResponse = authTokenService.issueTokens(user)

        assertNotNull(refreshTokenRepository.findByToken(tokenResponse.refreshToken))
        assertEquals(user.id, tokenResponse.userId)
    }

    @Test
    fun `refresh rotates refresh token`() {
        val user = userRepository.save(testUser("rotate"))
        val issued = authTokenService.issueTokens(user)

        val refreshed = authTokenService.refresh("Bearer ${issued.refreshToken}")

        assertNull(refreshTokenRepository.findByToken(issued.refreshToken))
        assertNotNull(refreshTokenRepository.findByToken(refreshed.refreshToken))
        assertNotEquals(issued.refreshToken, refreshed.refreshToken)
    }

    @Test
    fun `logout without refresh token header invalidates all refresh tokens for user`() {
        val user = userRepository.save(testUser("logout"))
        authTokenService.issueTokens(user)
        authTokenService.issueTokens(user)

        authTokenService.logout(user.id!!, null)

        assertEquals(0, refreshTokenRepository.findAll().size)
    }

    private fun testUser(suffix: String): User = User(
        provider = UserAuthProvider.KAKAO,
        oidcIssuer = "https://kauth.kakao.com",
        oidcSubject = "subject-$suffix",
        nickname = "tester-$suffix",
        email = "tester-$suffix@example.com"
    )

    @TestConfiguration
    class TestConfig {
        @Bean
        fun jwtProperties(): JwtProperties = JwtProperties(
            issuer = "https://polaris.test",
            secret = "polaris-auth-jwt-secret-key-polstar-2026-32bytes",
            accessTokenExpirationSeconds = 3600,
            refreshTokenExpirationSeconds = 1209600
        )
    }
}
