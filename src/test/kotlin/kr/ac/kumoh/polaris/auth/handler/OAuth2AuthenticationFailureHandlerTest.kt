package kr.ac.kumoh.polaris.auth.handler

import kr.ac.kumoh.polaris.auth.config.AppLoginAuthorizationRequestRepository
import kr.ac.kumoh.polaris.auth.config.AppLoginChannel
import kr.ac.kumoh.polaris.auth.config.AppLoginContext
import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.AuthenticationException
import tools.jackson.databind.ObjectMapper

class OAuth2AuthenticationFailureHandlerTest {
    @Test
    fun `web callback failure redirects back to target with error details`() {
        val repository = mock(AppLoginAuthorizationRequestRepository::class.java)
        val handler = OAuth2AuthenticationFailureHandler(
            authAppLoginProperties = AuthAppLoginProperties(
                app = AuthAppLoginProperties.App(
                    targets = mapOf("web" to "http://localhost:3000/login")
                )
            ),
            appLoginAuthorizationRequestRepository = repository,
            objectMapper = ObjectMapper()
        )
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val context = AppLoginContext(
            channel = AppLoginChannel.WEB,
            targetId = "web",
            correlationId = "corr-failure"
        )

        `when`(repository.removeAppLoginContext(request)).thenReturn(context)

        handler.onAuthenticationFailure(request, response, object : AuthenticationException("boom") {})

        assertEquals(303, response.status)
        val location = response.getHeader("Location")
        assertTrue(location!!.startsWith("http://localhost:3000/login?"))
        assertTrue(location.contains("error=login_failed"))
        assertTrue(location.contains("errorCode=OIDC_LOGIN_FAILED"))
        assertTrue(location.contains("correlationId=corr-failure"))
    }
}
