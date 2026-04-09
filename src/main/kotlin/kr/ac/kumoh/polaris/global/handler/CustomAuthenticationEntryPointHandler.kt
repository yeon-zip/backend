package kr.ac.kumoh.polaris.global.handler

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import tools.jackson.databind.ObjectMapper
import java.io.IOException

class CustomAuthenticationEntryPointHandler(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {
    private val logger = LoggerFactory.getLogger(this::class.simpleName)

    @Throws(IOException::class, ServletException::class)
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED).apply {
            title = HttpStatus.UNAUTHORIZED.reasonPhrase
            detail = "인증이 필요합니다."
        }

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()

        objectMapper.writeValue(response.writer, problemDetail)
    }
}
