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
import kr.ac.kumoh.polaris.global.dto.CursorPageResponse
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.notification.presentation.dto.CreateNotificationSubscriptionRequest
import kr.ac.kumoh.polaris.notification.presentation.dto.NotificationCountResponse
import kr.ac.kumoh.polaris.notification.presentation.dto.NotificationSubscriptionResponse
import kr.ac.kumoh.polaris.notification.presentation.dto.UserNotificationResponse
import kr.ac.kumoh.polaris.notification.service.NotificationSubscriptionService
import kr.ac.kumoh.polaris.notification.service.UserNotificationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "알림")
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val userNotificationService: UserNotificationService,
    private val notificationSubscriptionService: NotificationSubscriptionService
) {
    @Operation(
        summary = "알림 개수 조회",
        description = "<p>현재 로그인한 사용자의 삭제되지 않은 알림 개수를 조회합니다.</p>",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공"),
            ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
        ]
    )
    @GetMapping("/count")
    fun countNotifications(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<NotificationCountResponse> =
        ResponseEntity.ok(
            NotificationCountResponse(
                count = userNotificationService.countVisible(authenticatedUser.id)
            )
        )

    @Operation(
        summary = "알림 목록 조회",
        description = "<p>현재 로그인한 사용자의 알림 목록을 최신순으로 조회합니다. cursor와 limit을 사용해 다음 페이지를 조회할 수 있습니다. 삭제된 알림은 조회되지 않습니다.</p>",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @Parameters(
        value = [
            Parameter(
                name = "cursor",
                description = "커서 이후의 알림만 조회하려면 이전 응답의 nextCursor 값을 입력하세요. 없으면 첫 페이지를 조회합니다."
            ),
            Parameter(
                name = "limit",
                description = "한 번에 조회할 알림 개수입니다. 1부터 100까지 입력할 수 있으며 기본값은 20입니다."
            )
        ]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공"),
            ApiResponse(responseCode = "400", description = "cursor 또는 limit 값이 올바르지 않습니다."),
            ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
        ]
    )
    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<CursorPageResponse<UserNotificationResponse>> {
        val result = userNotificationService.getVisibleNotifications(
            userId = authenticatedUser.id,
            cursor = cursor,
            limit = limit
        )

        return ResponseEntity.ok(CursorPageResponse.from(result, UserNotificationResponse::from))
    }

    @Operation(
        summary = "알림 삭제",
        description = "<p>사용자의 알림을 삭제합니다. 알림 받기 설정은 삭제되지 않습니다.</p>",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
            ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            ApiResponse(responseCode = "403", description = "다른 사용자의 알림은 삭제할 수 없습니다."),
            ApiResponse(responseCode = "404", description = "알림을 찾을 수 없습니다.")
        ]
    )
    @DeleteMapping("/{notificationId}")
    fun deleteNotification(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Parameter(description = "삭제할 알림을 식별하는 ID입니다.", example = "123")
        @PathVariable notificationId: Long
    ): ResponseEntity<Void> {
        userNotificationService.deleteNotification(
            userId = authenticatedUser.id,
            notificationId = notificationId
        )
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "알림 받기 설정 등록",
        description = "<p>특정 도서가 특정 도서관에서 대출 가능해졌을 때 알림을 받을 수 있도록 설정합니다. 이미 등록된 활성 알림 받기 설정이 있으면 중복 등록할 수 없습니다. 비활성화된 기존 설정이 있으면 다시 활성화될 수 있습니다. 실제 알림은 서버가 대출 가능 여부를 확인한 뒤 생성합니다.</p>",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
            ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않습니다."),
            ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            ApiResponse(responseCode = "404", description = "도서 또는 도서관을 찾을 수 없습니다."),
            ApiResponse(responseCode = "409", description = "이미 등록된 활성 알림 받기 설정입니다.")
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
        summary = "내 알림 받기 설정 조회",
        description = "<p>현재 로그인한 사용자의 활성 알림 받기 설정 목록을 조회합니다. 도서와 도서관 정보, 마지막 확인 상태를 함께 반환합니다.</p>",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공"),
            ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
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
        summary = "알림 받기 설정 해제",
        description = "<p>특정 도서와 도서관에 대한 알림 받기 설정을 해제합니다. 이미 생성된 알림 기록은 삭제되지 않습니다. 해제할 설정이 없어도 요청은 성공합니다.</p>",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @Parameters(
        value = [
            Parameter(name = "isbn", description = "해제할 알림 받기 설정의 도서 ISBN입니다."),
            Parameter(name = "libraryId", description = "해제할 알림 받기 설정의 도서관 ID입니다.")
        ]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
            ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않습니다."),
            ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
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
