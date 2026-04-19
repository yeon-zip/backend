package kr.ac.kumoh.polaris.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.ac.kumoh.polaris.auth.presentation.response.OidcLoginResponse
import kr.ac.kumoh.polaris.auth.principal.PolarisOidcUser
import kr.ac.kumoh.polaris.auth.service.AuthTokenService
import kr.ac.kumoh.polaris.user.presentation.response.CurrentUserResponse
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class OAuth2AuthenticationSuccessHandler(
    private val authTokenService: AuthTokenService,
    private val objectMapper: ObjectMapper
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal as PolarisOidcUser
        val tokenResponse = authTokenService.issueTokens(principal.user)
        val body = OidcLoginResponse(
            accessToken = tokenResponse.accessToken,
            refreshToken = tokenResponse.refreshToken,
            expiresIn = tokenResponse.expiresIn,
            user = CurrentUserResponse(
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
