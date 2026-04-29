package kr.ac.kumoh.polaris.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.ac.kumoh.polaris.auth.config.AppLoginAuthorizationRequestRepository
import kr.ac.kumoh.polaris.auth.config.AppLoginContext
import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.auth.principal.PolarisOidcUser
import kr.ac.kumoh.polaris.auth.service.LoginExchangeCodeService
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
/**
 * OAuth/OIDC 로그인 성공 후 access/refresh token을 직접 반환하지 않고,
 * target URI로 교환용 exchange code를 포함한 303 redirect를 보낸다.
 */
class OAuth2AuthenticationSuccessHandler(
    private val loginExchangeCodeService: LoginExchangeCodeService,
    private val authAppLoginProperties: AuthAppLoginProperties,
    private val appLoginAuthorizationRequestRepository: AppLoginAuthorizationRequestRepository
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal as PolarisOidcUser
        val appLoginContext = requireLoginContext(request)
        val targetId = appLoginContext.targetId ?: throw IllegalStateException("OIDC 로그인 targetId가 없습니다.")
        val targetUri = requireTargetUri(targetId)

        val exchangeCode = loginExchangeCodeService.issueExchangeCode(
            user = principal.user,
            targetId = targetId,
            codeChallenge = appLoginContext.codeChallenge,
            correlationId = appLoginContext.correlationId
        )

        response.status = HttpServletResponse.SC_SEE_OTHER
        response.setHeader("Location", buildExchangeRedirectUri(targetUri, targetId, exchangeCode, appLoginContext.correlationId))
    }

    private fun requireLoginContext(request: HttpServletRequest): AppLoginContext =
        appLoginAuthorizationRequestRepository.removeAppLoginContext(request)
            ?: throw IllegalStateException("OIDC 로그인 컨텍스트가 없습니다.")

    private fun requireTargetUri(targetId: String): String = authAppLoginProperties.resolveTarget(targetId)
        ?: throw IllegalStateException("허용되지 않은 로그인 대상입니다: $targetId")

    private fun buildExchangeRedirectUri(
        targetUri: String,
        targetId: String,
        exchangeCode: String,
        correlationId: String
    ): String = UriComponentsBuilder.fromUriString(targetUri)
        .queryParam("code", exchangeCode)
        .queryParam("targetId", targetId)
        .queryParam("correlationId", correlationId)
        .build(true)
        .toUriString()
}
