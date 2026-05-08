package kr.ac.kumoh.polaris.notification.presentation.controller

import kr.ac.kumoh.polaris.auth.config.AppLoginAuthorizationRequestRepository
import kr.ac.kumoh.polaris.auth.handler.OAuth2AuthenticationFailureHandler
import kr.ac.kumoh.polaris.auth.handler.OAuth2AuthenticationSuccessHandler
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.auth.service.KakaoOidcUserService
import kr.ac.kumoh.polaris.auth.util.JwtTokenProvider
import kr.ac.kumoh.polaris.global.config.SecurityConfig
import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.GlobalExceptionHandler
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.notification.entity.NotificationType
import kr.ac.kumoh.polaris.notification.service.NotificationSubscriptionService
import kr.ac.kumoh.polaris.notification.service.UserNotificationResult
import kr.ac.kumoh.polaris.notification.service.UserNotificationService
import kr.ac.kumoh.polaris.user.entity.UserRole
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(NotificationController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class NotificationControllerTest(
    @Autowired private val mockMvc: MockMvc
) {
    @MockitoBean
    private lateinit var userNotificationService: UserNotificationService

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
        mockMvc.perform(get("/api/v1/notifications/count"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/v1/notifications"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(delete("/api/v1/notifications/1"))
            .andExpect(status().isUnauthorized)

        verifyNoInteractions(userNotificationService, notificationSubscriptionService)
    }

    @Test
    fun `create subscription route rejects unauthenticated requests`() {
        mockMvc.perform(
            post("/api/v1/notifications/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"isbn":"9791191111111","libraryId":10}""")
        ).andExpect(status().isUnauthorized)

        verifyNoInteractions(userNotificationService, notificationSubscriptionService)
    }

    @Test
    fun `my subscriptions route rejects unauthenticated requests`() {
        mockMvc.perform(get("/api/v1/notifications/subscriptions/me"))
            .andExpect(status().isUnauthorized)

        verifyNoInteractions(userNotificationService, notificationSubscriptionService)
    }

    @Test
    fun `unsubscribe route rejects unauthenticated requests`() {
        mockMvc.perform(
            delete("/api/v1/notifications/subscriptions")
                .param("isbn", "9791191111111")
                .param("libraryId", "10")
        ).andExpect(status().isUnauthorized)

        verifyNoInteractions(userNotificationService, notificationSubscriptionService)
    }

    @Test
    fun `authenticated user can count and list notifications`() {
        `when`(userNotificationService.countVisible(7L)).thenReturn(3L)
        `when`(userNotificationService.getVisibleNotifications(7L, "123", 20)).thenReturn(
            CursorPageResult(
                hasNext = false,
                nextCursor = null,
                items = listOf(
                    UserNotificationResult(
                        notificationId = 122L,
                        notificationType = NotificationType.BOOK_AVAILABLE,
                        isbn = "9791191111111",
                        bookTitle = "테스트 도서",
                        libraryId = 10L,
                        libraryName = "구미시립양포도서관",
                        title = "대출 가능 알림",
                        message = "알림 받기 한 도서가 구미시립양포도서관에서 대출 가능합니다.",
                        notificationDate = LocalDate.of(2026, 5, 7),
                        createdAt = LocalDateTime.parse("2026-05-07T09:00:01")
                    )
                )
            )
        )

        mockMvc.perform(
            get("/api/v1/notifications/count")
                .with(authentication(authenticatedUserAuthentication(7L)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(3))

        mockMvc.perform(
            get("/api/v1/notifications")
                .with(authentication(authenticatedUserAuthentication(7L)))
                .param("cursor", "123")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].notificationId").value(122))
            .andExpect(jsonPath("$.items[0].notificationType").value("BOOK_AVAILABLE"))
            .andExpect(jsonPath("$.items[0].libraryName").value("구미시립양포도서관"))

        verify(userNotificationService).countVisible(7L)
        verify(userNotificationService).getVisibleNotifications(7L, "123", 20)
    }

    @Test
    fun `authenticated user can delete own notification`() {
        mockMvc.perform(
            delete("/api/v1/notifications/11")
                .with(authentication(authenticatedUserAuthentication(7L)))
        ).andExpect(status().isNoContent)

        verify(userNotificationService).deleteNotification(7L, 11L)
    }

    @Test
    fun `delete other user notification returns forbidden`() {
        doThrow(
            ServiceException(
                errorCode = ErrorCode.NOTIFICATION_ACCESS_DENIED,
                message = "다른 사용자의 알림에는 접근할 수 없습니다."
            )
        ).`when`(userNotificationService).deleteNotification(7L, 11L)

        mockMvc.perform(
            delete("/api/v1/notifications/11")
                .with(authentication(authenticatedUserAuthentication(7L)))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_ACCESS_DENIED"))
    }

    @Test
    fun `delete missing notification returns not found`() {
        doThrow(
            ServiceException(
                errorCode = ErrorCode.NOTIFICATION_NOT_FOUND,
                message = "알림을 찾을 수 없습니다."
            )
        ).`when`(userNotificationService).deleteNotification(7L, 404L)

        mockMvc.perform(
            delete("/api/v1/notifications/404")
                .with(authentication(authenticatedUserAuthentication(7L)))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_NOT_FOUND"))
    }

    @Test
    fun `authenticated user can create subscription and token api is absent`() {
        mockMvc.perform(
            post("/api/v1/notifications/subscriptions")
                .with(authentication(authenticatedUserAuthentication(7L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"isbn":"9791191111111","libraryId":10}""")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/v1/notifications/tokens")
                .with(authentication(authenticatedUserAuthentication(7L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"platform":"IOS","deviceToken":"token"}""")
        ).andExpect(status().isMethodNotAllowed)

        verify(notificationSubscriptionService).subscribe(7L, "9791191111111", 10L)
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
