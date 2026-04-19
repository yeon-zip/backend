package kr.ac.kumoh.polaris.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.ac.kumoh.polaris.auth.config.AppLoginAuthorizationRequestRepository
import kr.ac.kumoh.polaris.auth.config.AppLoginChannel
import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.auth.presentation.response.OidcLoginResponse
import kr.ac.kumoh.polaris.auth.principal.PolarisOidcUser
import kr.ac.kumoh.polaris.auth.service.AuthTokenService
import kr.ac.kumoh.polaris.auth.service.LoginExchangeCodeService
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.ObjectMapper

@Component
class OAuth2AuthenticationSuccessHandler(
    private val authTokenService: AuthTokenService,
    private val loginExchangeCodeService: LoginExchangeCodeService,
    private val authAppLoginProperties: AuthAppLoginProperties,
    private val appLoginAuthorizationRequestRepository: AppLoginAuthorizationRequestRepository,
    private val objectMapper: ObjectMapper
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal as PolarisOidcUser
        val appLoginContext = appLoginAuthorizationRequestRepository.removeAppLoginContext(request)

        if (appLoginContext?.channel == AppLoginChannel.APP) {
            val targetId = appLoginContext.targetId ?: throw IllegalStateException("앱 로그인 targetId가 없습니다.")
            val targetUri = authAppLoginProperties.resolveTarget(targetId)
                ?: throw IllegalStateException("허용되지 않은 앱 로그인 대상입니다: $targetId")
            val exchangeCode = loginExchangeCodeService.issueExchangeCode(
                user = principal.user,
                targetId = targetId,
                codeChallenge = appLoginContext.codeChallenge,
                correlationId = appLoginContext.correlationId
            )
            val redirectUri = UriComponentsBuilder.fromUriString(targetUri)
                .queryParam("code", exchangeCode)
                .queryParam("targetId", targetId)
                .queryParam("correlationId", appLoginContext.correlationId)
                .build(true)
                .toUriString()

            response.status = HttpServletResponse.SC_SEE_OTHER
            response.setHeader("Location", redirectUri)
            return
        }

        if (!authAppLoginProperties.legacyJsonCallback.enabled) {
            val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN).apply {
                title = HttpStatus.FORBIDDEN.reasonPhrase
                detail = "Non-app OIDC callback is disabled. Enable core.auth.oidc.legacy-json-callback.enabled or use the app exchange flow."
                setProperty("errorCode", ErrorCode.OIDC_NON_APP_CALLBACK_DISABLED.name)
            }
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
            response.characterEncoding = Charsets.UTF_8.name()
            objectMapper.writeValue(response.writer, problemDetail)
            return
        }

        val tokenResponse = authTokenService.issueTokens(principal.user)
        val body = OidcLoginResponse(
            accessToken = tokenResponse.accessToken,
            refreshToken = tokenResponse.refreshToken,
            expiresIn = tokenResponse.expiresIn,
            user = kr.ac.kumoh.polaris.user.presentation.response.CurrentUserResponse(
                id = principal.user.id ?: throw IllegalStateException("사용자 ID가 없습니다."),
                provider = principal.user.provider,
                role = principal.user.role,
                nickname = principal.user.nickname,
                email = principal.user.email,
                profileImageUrl = principal.user.profileImageUrl
            )
        )

        response.status = HttpServletResponse.SC_OK
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.writer, body)
    }
}
