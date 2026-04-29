package kr.ac.kumoh.polaris.auth.handler

import kr.ac.kumoh.polaris.auth.config.AppLoginAuthorizationRequestRepository
import kr.ac.kumoh.polaris.auth.config.AppLoginChannel
import kr.ac.kumoh.polaris.auth.config.AppLoginContext
import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.auth.principal.PolarisOidcUser
import kr.ac.kumoh.polaris.auth.service.LoginExchangeCodeService
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import java.time.Duration
import java.time.Instant

class OAuth2AuthenticationSuccessHandlerTest {
    @Test
    fun `web callback redirects with exchange code instead of returning JSON tokens`() {
        val exchangeCodeService = mock(LoginExchangeCodeService::class.java)
        val repository = mock(AppLoginAuthorizationRequestRepository::class.java)
        val handler = OAuth2AuthenticationSuccessHandler(
            loginExchangeCodeService = exchangeCodeService,
            authAppLoginProperties = authAppLoginProperties(),
            appLoginAuthorizationRequestRepository = repository
        )
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val user = testUser()
        val context = AppLoginContext(
            channel = AppLoginChannel.WEB,
            targetId = "web",
            codeChallenge = VALID_CODE_CHALLENGE,
            codeChallengeMethod = "S256",
            correlationId = "corr-web"
        )
        val authentication = UsernamePasswordAuthenticationToken(principal(user), null, emptyList())

        `when`(repository.removeAppLoginContext(request)).thenReturn(context)
        `when`(exchangeCodeService.issueExchangeCode(user, "web", VALID_CODE_CHALLENGE, "corr-web"))
            .thenReturn("exchange-code")

        handler.onAuthenticationSuccess(request, response, authentication)

        assertEquals(303, response.status)
        val location = response.getHeader("Location")
        assertTrue(location!!.startsWith("http://localhost:3000/login?"))
        assertTrue(location.contains("code=exchange-code"))
        assertTrue(location.contains("targetId=web"))
        assertTrue(location.contains("correlationId=corr-web"))
        verify(exchangeCodeService).issueExchangeCode(user, "web", VALID_CODE_CHALLENGE, "corr-web")
    }

    private fun principal(user: User): PolarisOidcUser = PolarisOidcUser(
        user = user,
        authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
        idToken = OidcIdToken(
            "id-token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            mapOf("sub" to user.oidcSubject)
        ),
        userInfo = null
    )

    private fun testUser(): User = User(
        provider = UserAuthProvider.KAKAO,
        oidcIssuer = "https://kauth.kakao.com",
        oidcSubject = "subject-web",
        nickname = "tester"
    )

    private fun authAppLoginProperties(): AuthAppLoginProperties = AuthAppLoginProperties(
        app = AuthAppLoginProperties.App(
            targets = mapOf(
                "web" to "http://localhost:3000/login",
                "mobile" to "polaris://auth/callback"
            ),
            exchange = AuthAppLoginProperties.Exchange(
                ttl = Duration.ofMinutes(3),
                allowWithoutProof = false
            )
        )
    )

    companion object {
        private const val VALID_CODE_CHALLENGE = "abcdefghijklmnopqrstuvwxyzABCDE1234567890_-"
    }
}
