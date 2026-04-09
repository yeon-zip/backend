package kr.ac.kumoh.polaris.global.handler

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import tools.jackson.databind.ObjectMapper
import java.io.IOException

class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper
) : AccessDeniedHandler {
    private val logger = LoggerFactory.getLogger(this::class.simpleName)

    @Throws(IOException::class, ServletException::class)
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN).apply {
            title = HttpStatus.FORBIDDEN.reasonPhrase
            detail = "권한이 없습니다."
        }

        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()

        objectMapper.writeValue(response.writer, problemDetail)
    }
}
