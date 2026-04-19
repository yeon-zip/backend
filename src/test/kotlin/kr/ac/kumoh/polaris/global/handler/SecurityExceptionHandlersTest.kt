package kr.ac.kumoh.polaris.global.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.test.json.JsonContentAssert
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

        assertEquals(401, response.status)
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, response.contentType)
        JsonContentAssert(this::class.java, response.contentAsString).apply {
            hasPathSatisfying("$.title") { assertEquals("Unauthorized", it) }
            hasPathSatisfying("$.detail") { assertEquals("인증이 필요합니다.", it) }
        }
    }

    @Test
    fun `access denied handler returns 403 problem detail`() {
        val response = MockHttpServletResponse()

        CustomAccessDeniedHandler(objectMapper).handle(
            MockHttpServletRequest(),
            response,
            AccessDeniedException("forbidden"),
        )

        assertEquals(403, response.status)
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, response.contentType)
        JsonContentAssert(this::class.java, response.contentAsString).apply {
            hasPathSatisfying("$.title") { assertEquals("Forbidden", it) }
            hasPathSatisfying("$.detail") { assertEquals("권한이 없습니다.", it) }
        }
    }
}
