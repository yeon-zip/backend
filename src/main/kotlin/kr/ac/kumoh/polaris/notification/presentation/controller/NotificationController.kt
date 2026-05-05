package kr.ac.kumoh.polaris.notification.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "알림")
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val pushTokenService: PushTokenService,
    private val notificationSubscriptionService: NotificationSubscriptionService
) {
    @Operation(
        summary = "푸시 토큰 등록",
        description = """
            <p>인증된 사용자의 앱 푸시 토큰을 등록하거나 다시 활성화합니다.</p>
            <p>사용자가 도서 대출 가능 알림을 받을 수 있도록 플랫폼과 기기 토큰을 서버에 연결하는 역할을 합니다.</p>
            <p>플랫폼 값은 <code>ANDROID</code>, <code>IOS</code>만 허용됩니다.</p>
        """,
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
        ]
    )
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

    @Operation(
        summary = "푸시 토큰 삭제",
        description = """
            <p>인증된 사용자의 앱 푸시 토큰을 비활성화합니다.</p>
            <p>사용자가 더 이상 해당 기기로 알림을 받지 않도록 플랫폼과 기기 토큰의 연결을 해제하는 역할을 합니다.</p>
        """,
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
        ]
    )
    @Parameters(
        value = [
            Parameter(name = "platform", description = "삭제하려는 푸시 토큰의 플랫폼입니다. <code>ANDROID</code>, <code>IOS</code> 중 하나를 사용합니다."),
            Parameter(name = "deviceToken", description = "삭제하려는 기기의 푸시 토큰입니다.")
        ]
    )
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

    @Operation(
        summary = "도서 대출 가능 알림 구독 등록",
        description = """
            <p>인증된 사용자가 특정 도서와 도서관 조합에 대한 대출 가능 알림을 구독합니다.</p>
            <p>구독한 도서가 해당 도서관에서 대출 가능 상태가 되면 알림 발송 대상이 되도록 등록하는 역할을 합니다.</p>
            <p>이미 비활성화된 구독이 있으면 새 구독을 만들지 않고 다시 활성화합니다.</p>
        """,
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
        ]
    )
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

    @Operation(
        summary = "내 알림 구독 목록 조회",
        description = """
            <p>인증된 사용자가 등록한 도서 대출 가능 알림 구독 목록을 조회합니다.</p>
            <p>사용자가 어떤 도서와 도서관 조합에 대해 알림을 받고 있는지 확인하는 역할을 합니다.</p>
        """,
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공"),
        ]
    )
    @GetMapping("/subscriptions/me")
    fun getMySubscriptions(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<List<NotificationSubscriptionResponse>> =
        ResponseEntity.ok(
            notificationSubscriptionService.getMySubscriptions(authenticatedUser.id)
                .map(NotificationSubscriptionResponse::from)
        )

    @Operation(
        summary = "도서 대출 가능 알림 구독 삭제",
        description = """
            <p>인증된 사용자가 특정 도서와 도서관 조합에 대한 대출 가능 알림 구독을 비활성화합니다.</p>
            <p>사용자가 더 이상 해당 도서와 도서관 조합의 대출 가능 알림을 받지 않도록 구독을 해제하는 역할을 합니다.</p>
        """,
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
        ]
    )
    @Parameters(
        value = [
            Parameter(name = "isbn", description = "구독을 삭제하려는 도서의 ISBN입니다. 13자리 값이어야 합니다."),
            Parameter(name = "libraryId", description = "구독을 삭제하려는 도서관의 ID입니다.")
        ]
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
