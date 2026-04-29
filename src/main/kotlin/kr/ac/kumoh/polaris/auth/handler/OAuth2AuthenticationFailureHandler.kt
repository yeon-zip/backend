package kr.ac.kumoh.polaris.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.ac.kumoh.polaris.auth.config.AppLoginAuthorizationRequestRepository
import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.ObjectMapper

@Component
/**
 * OAuth/OIDC 로그인 실패 시 가능한 경우 target URI로 에러 정보를 redirect하고,
 * target을 결정할 수 없을 때만 Problem JSON을 직접 반환한다.
 */
class OAuth2AuthenticationFailureHandler(
    private val authAppLoginProperties: AuthAppLoginProperties,
    private val appLoginAuthorizationRequestRepository: AppLoginAuthorizationRequestRepository,
    private val objectMapper: ObjectMapper
) : AuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        val appLoginContext = appLoginAuthorizationRequestRepository.removeAppLoginContext(request)
        val redirectUri = appLoginContext?.targetId
            ?.let(authAppLoginProperties::resolveTarget)
            ?.let { targetUri ->
                UriComponentsBuilder.fromUriString(targetUri)
                    .queryParam("error", LOGIN_FAILED)
                    .queryParam("errorCode", ErrorCode.OIDC_LOGIN_FAILED.name)
                    .queryParam("correlationId", appLoginContext.correlationId)
                    .build(true)
                    .toUriString()
            }
        if (redirectUri != null) {
            response.status = HttpServletResponse.SC_SEE_OTHER
            response.setHeader("Location", redirectUri)
            return
        }

        val problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED).apply {
            title = HttpStatus.UNAUTHORIZED.reasonPhrase
            detail = "Kakao OIDC 로그인에 실패했습니다. ${exception.message.orEmpty()}".trim()
            setProperty("errorCode", ErrorCode.OIDC_LOGIN_FAILED.name)
        }

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.writer, problemDetail)
    }

    companion object {
        private const val LOGIN_FAILED = "login_failed"
    }
}
