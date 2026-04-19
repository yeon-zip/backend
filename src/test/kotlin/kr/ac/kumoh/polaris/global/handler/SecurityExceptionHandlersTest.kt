package kr.ac.kumoh.polaris.global.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.InsufficientAuthenticationException
import tools.jackson.databind.ObjectMapper

class SecurityExceptionHandlersTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `authentication entry point returns 401 problem detail`() {
        val response = MockHttpServletResponse()

        CustomAuthenticationEntryPointHandler(objectMapper).commence(
            MockHttpServletRequest(),
            response,
            InsufficientAuthenticationException("auth required"),
        )

        val payload = objectMapper.readTree(response.contentAsString)

        assertEquals(401, response.status)
        assertEquals(true, response.contentType?.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE) == true)
        assertEquals("Unauthorized", payload["title"].textValue())
        assertEquals("인증이 필요합니다.", payload["detail"].textValue())
    }

    @Test
    fun `access denied handler returns 403 problem detail`() {
        val response = MockHttpServletResponse()

        CustomAccessDeniedHandler(objectMapper).handle(
            MockHttpServletRequest(),
            response,
            AccessDeniedException("forbidden"),
        )

        val payload = objectMapper.readTree(response.contentAsString)

        assertEquals(403, response.status)
        assertEquals(true, response.contentType?.startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE) == true)
        assertEquals("Forbidden", payload["title"].textValue())
        assertEquals("권한이 없습니다.", payload["detail"].textValue())
    }
}
