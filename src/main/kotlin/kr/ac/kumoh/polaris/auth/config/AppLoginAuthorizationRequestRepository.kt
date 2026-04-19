package kr.ac.kumoh.polaris.auth.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component

@Component
class AppLoginAuthorizationRequestRepository : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private val delegate = HttpSessionOAuth2AuthorizationRequestRepository()

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        delegate.loadAuthorizationRequest(request)

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        delegate.saveAuthorizationRequest(authorizationRequest, request, response)
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): OAuth2AuthorizationRequest? = delegate.removeAuthorizationRequest(request, response)

    fun saveAppLoginContext(
        request: HttpServletRequest,
        appLoginContext: AppLoginContext
    ) {
        request.session.setAttribute(APP_LOGIN_CONTEXT_SESSION_ATTRIBUTE, appLoginContext)
    }

    fun loadAppLoginContext(request: HttpServletRequest): AppLoginContext? =
        request.session.getAttribute(APP_LOGIN_CONTEXT_SESSION_ATTRIBUTE) as? AppLoginContext

    fun removeAppLoginContext(request: HttpServletRequest): AppLoginContext? =
        loadAppLoginContext(request)?.also {
            request.session.removeAttribute(APP_LOGIN_CONTEXT_SESSION_ATTRIBUTE)
        }

    companion object {
        private const val APP_LOGIN_CONTEXT_SESSION_ATTRIBUTE =
            "kr.ac.kumoh.polaris.auth.APP_LOGIN_CONTEXT"
    }
}
