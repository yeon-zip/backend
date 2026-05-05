package kr.ac.kumoh.polaris.notification.presentation.controller

import jakarta.validation.Valid
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.notification.entity.PushPlatform
import kr.ac.kumoh.polaris.notification.presentation.dto.CreateNotificationSubscriptionRequest
import kr.ac.kumoh.polaris.notification.presentation.dto.NotificationSubscriptionResponse
import kr.ac.kumoh.polaris.notification.presentation.dto.RegisterPushTokenRequest
import kr.ac.kumoh.polaris.notification.service.NotificationSubscriptionService
import kr.ac.kumoh.polaris.notification.service.PushTokenService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val pushTokenService: PushTokenService,
    private val notificationSubscriptionService: NotificationSubscriptionService
) {
    @PostMapping("/tokens")
    fun registerToken(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Valid @RequestBody request: RegisterPushTokenRequest
    ): ResponseEntity<Void> {
        pushTokenService.registerToken(
            userId = authenticatedUser.id,
            platform = request.platform ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE),
            deviceToken = request.deviceToken ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        )
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/tokens")
    fun deleteToken(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @RequestParam platform: PushPlatform,
        @RequestParam deviceToken: String
    ): ResponseEntity<Void> {
        pushTokenService.deactivateToken(
            userId = authenticatedUser.id,
            platform = platform,
            deviceToken = deviceToken
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/subscriptions")
    fun createSubscription(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Valid @RequestBody request: CreateNotificationSubscriptionRequest
    ): ResponseEntity<Void> {
        notificationSubscriptionService.subscribe(
            userId = authenticatedUser.id,
            isbn = request.isbn ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE),
            libraryId = request.libraryId ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        )
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/subscriptions/me")
    fun getMySubscriptions(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<List<NotificationSubscriptionResponse>> =
        ResponseEntity.ok(
            notificationSubscriptionService.getMySubscriptions(authenticatedUser.id)
                .map(NotificationSubscriptionResponse::from)
        )

    @DeleteMapping("/subscriptions")
    fun deleteSubscription(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @RequestParam isbn: String,
        @RequestParam libraryId: Long
    ): ResponseEntity<Void> {
        notificationSubscriptionService.unsubscribe(
            userId = authenticatedUser.id,
            isbn = isbn,
            libraryId = libraryId
        )
        return ResponseEntity.noContent().build()
    }
}
