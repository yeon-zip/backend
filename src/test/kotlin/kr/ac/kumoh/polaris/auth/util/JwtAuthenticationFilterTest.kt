package kr.ac.kumoh.polaris.auth.util

import kr.ac.kumoh.polaris.auth.config.JwtProperties
import kr.ac.kumoh.polaris.auth.filter.JwtAuthenticationFilter
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import tools.jackson.databind.ObjectMapper

class JwtAuthenticationFilterTest {
    private val jwtTokenProvider = JwtTokenProvider(
        JwtProperties(
            issuer = "https://polaris.test",
            secret = "polaris-auth-jwt-secret-key-polstar-2026-32bytes",
            accessTokenExpirationSeconds = 3600,
            refreshTokenExpirationSeconds = 1209600
        )
    )
    private val filter = JwtAuthenticationFilter(jwtTokenProvider, ObjectMapper())

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `access token authenticates request`() {
        val user = testUser(1L)
        val accessToken = jwtTokenProvider.generateAccessToken(user)
        val request = MockHttpServletRequest("GET", "/api/v1/users/me").apply {
            addHeader("Authorization", "Bearer $accessToken")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        val principal = SecurityContextHolder.getContext().authentication?.principal as AuthenticatedUser
        assertEquals(1L, principal.id)
        assertEquals(200, response.status)
    }

    @Test
    fun `refresh endpoint skips jwt access token parsing`() {
        val user = testUser(2L)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user)
        val request = MockHttpServletRequest("POST", "/api/v1/auth/refresh").apply {
            addHeader("Authorization", "Bearer $refreshToken")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertNull(SecurityContextHolder.getContext().authentication)
        assertEquals(200, response.status)
    }


    @Test
    fun `exchange endpoint skips jwt parsing even with invalid bearer token`() {
        val request = MockHttpServletRequest("POST", "/api/v1/auth/exchange").apply {
            addHeader("Authorization", "Bearer invalid-token")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertNull(SecurityContextHolder.getContext().authentication)
        assertEquals(200, response.status)
    }

    @Test
    fun `exchange endpoint skips jwt access token parsing`() {
        val user = testUser(3L)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user)
        val request = MockHttpServletRequest("POST", "/api/v1/auth/exchange").apply {
            addHeader("Authorization", "Bearer $refreshToken")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertNull(SecurityContextHolder.getContext().authentication)
        assertEquals(200, response.status)
    }

    @Test
    fun `exchange endpoint skips jwt access token parsing`() {
        val user = testUser(3L)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user)
        val request = MockHttpServletRequest("POST", "/api/v1/auth/exchange").apply {
            addHeader("Authorization", "Bearer $refreshToken")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertNull(SecurityContextHolder.getContext().authentication)
        assertEquals(200, response.status)
    }

    @Test
    fun `invalid access token returns unauthorized`() {
        val request = MockHttpServletRequest("GET", "/api/v1/users/me").apply {
            addHeader("Authorization", "Bearer invalid-token")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertEquals(401, response.status)
    }

    private fun testUser(id: Long): User {
        val user = User(
            provider = UserAuthProvider.KAKAO,
            oidcIssuer = "https://kauth.kakao.com",
            oidcSubject = "subject-$id",
            nickname = "tester$id",
            email = "tester$id@example.com"
        )
        val field = User::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(user, id)
        return user
    }
}
