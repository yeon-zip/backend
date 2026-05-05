package kr.ac.kumoh.polaris.notification.presentation.controller

import kr.ac.kumoh.polaris.auth.config.AppLoginAuthorizationRequestRepository
import kr.ac.kumoh.polaris.auth.handler.OAuth2AuthenticationFailureHandler
import kr.ac.kumoh.polaris.auth.handler.OAuth2AuthenticationSuccessHandler
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.auth.service.KakaoOidcUserService
import kr.ac.kumoh.polaris.auth.util.JwtTokenProvider
import kr.ac.kumoh.polaris.global.config.SecurityConfig
import kr.ac.kumoh.polaris.global.exception.GlobalExceptionHandler
import kr.ac.kumoh.polaris.notification.entity.PushPlatform
import kr.ac.kumoh.polaris.notification.service.NotificationSubscriptionService
import kr.ac.kumoh.polaris.notification.service.PushTokenService
import kr.ac.kumoh.polaris.user.entity.UserRole
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(NotificationController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class NotificationControllerTest(
    @Autowired private val mockMvc: MockMvc
) {
    @MockitoBean
    private lateinit var pushTokenService: PushTokenService

    @MockitoBean
    private lateinit var notificationSubscriptionService: NotificationSubscriptionService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var kakaoOidcUserService: KakaoOidcUserService

    @MockitoBean
    private lateinit var oauth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler

    @MockitoBean
    private lateinit var oauth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler

    @MockitoBean
    private lateinit var appLoginAuthorizationRequestRepository: AppLoginAuthorizationRequestRepository

    @MockitoBean
    private lateinit var clientRegistrationRepository: ClientRegistrationRepository

    @Test
    fun `notification routes require authentication`() {
        mockMvc.perform(
            post("/api/v1/notifications/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"platform":"ANDROID","deviceToken":"token"}""")
        ).andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/v1/notifications/subscriptions/me"))
            .andExpect(status().isUnauthorized)

        verifyNoInteractions(pushTokenService, notificationSubscriptionService)
    }

    @Test
    fun `authenticated user can register and delete token`() {
        mockMvc.perform(
            post("/api/v1/notifications/tokens")
                .with(authentication(authenticatedUserAuthentication(7L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"platform":"ANDROID","deviceToken":"token"}""")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            delete("/api/v1/notifications/tokens")
                .with(authentication(authenticatedUserAuthentication(7L)))
                .param("platform", "ANDROID")
                .param("deviceToken", "token")
        ).andExpect(status().isNoContent)

        verify(pushTokenService).registerToken(7L, PushPlatform.ANDROID, "token")
        verify(pushTokenService).deactivateToken(7L, PushPlatform.ANDROID, "token")
    }

    private fun authenticatedUserAuthentication(userId: Long): UsernamePasswordAuthenticationToken {
        val principal = AuthenticatedUser(id = userId, role = UserRole.USER)
        return UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_${UserRole.USER.name}"))
        )
    }
}
