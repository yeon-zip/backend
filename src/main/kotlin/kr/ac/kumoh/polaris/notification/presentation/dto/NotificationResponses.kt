package kr.ac.kumoh.polaris.notification.presentation.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.ac.kumoh.polaris.notification.service.NotificationSubscriptionResult
import kr.ac.kumoh.polaris.notification.service.UserNotificationResult
import java.time.LocalDate
import java.time.LocalDateTime

data class NotificationCountResponse(
    @field:Schema(description = "삭제되지 않은 알림 개수입니다.", example = "3", nullable = false)
    val count: Long
)

data class UserNotificationResponse(
    @field:Schema(description = "알림을 식별하는 ID입니다.", example = "123", nullable = false)
    val notificationId: Long,

    @field:Schema(description = "알림 유형입니다.", example = "BOOK_AVAILABLE", nullable = false)
    val notificationType: String,

    @field:Schema(description = "알림 대상 도서를 식별하는 ISBN입니다.", example = "9791191111111", nullable = false)
    val isbn: String,

    @field:Schema(description = "알림 대상 도서명입니다.", example = "테스트 도서", nullable = false)
    val bookTitle: String,

    @field:Schema(description = "알림 대상 도서관을 식별하는 ID입니다.", example = "10", nullable = false)
    val libraryId: Long,

    @field:Schema(description = "알림 대상 도서관명입니다.", example = "구미시립양포도서관", nullable = false)
    val libraryName: String,

    @field:Schema(description = "알림 제목입니다.", example = "대출 가능 알림", nullable = false)
    val title: String,

    @field:Schema(
        description = "사용자에게 표시할 알림 내용입니다.",
        example = "알림 받기 한 도서가 구미시립양포도서관에서 대출 가능합니다.",
        nullable = false
    )
    val message: String,

    @field:Schema(description = "알림이 생성된 날짜입니다.", example = "2026-05-07", nullable = false)
    val notificationDate: LocalDate,

    @field:Schema(description = "알림이 생성된 시각입니다.", example = "2026-05-07T09:00:01", nullable = false)
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(result: UserNotificationResult): UserNotificationResponse =
            UserNotificationResponse(
                notificationId = result.notificationId,
                notificationType = result.notificationType.name,
                isbn = result.isbn,
                bookTitle = result.bookTitle,
                libraryId = result.libraryId,
                libraryName = result.libraryName,
                title = result.title,
                message = result.message,
                notificationDate = result.notificationDate,
                createdAt = result.createdAt
            )
    }
}

data class NotificationSubscriptionResponse(
    @field:Schema(description = "알림 받기 설정을 식별하는 ID입니다.", example = "15", nullable = false)
    val subscriptionId: Long,

    @field:Schema(description = "알림 받기 설정한 도서의 ISBN입니다.", example = "9791191111111", nullable = false)
    val isbn: String,

    @field:Schema(description = "알림 받기 설정한 도서명입니다.", example = "테스트 도서", nullable = false)
    val title: String,

    @field:Schema(description = "알림 받기 설정한 도서의 저자입니다.", example = "홍길동", nullable = true)
    val author: String?,

    @field:Schema(
        description = "알림 받기 설정한 도서의 표지 이미지 URL입니다.",
        example = "https://example.com/book-cover.jpg",
        nullable = true
    )
    val coverImageUrl: String?,

    @field:Schema(description = "알림 받기 설정한 도서관을 식별하는 ID입니다.", example = "10", nullable = false)
    val libraryId: Long,

    @field:Schema(description = "알림 받기 설정한 도서관명입니다.", example = "구미시립양포도서관", nullable = false)
    val libraryName: String,

    @field:Schema(
        description = "마지막으로 확정된 대출 가능 상태입니다. AVAILABLE, NOT_AVAILABLE, UNKNOWN 중 하나입니다.",
        example = "NOT_AVAILABLE",
        nullable = false
    )
    val lastStableAvailability: String,

    @field:Schema(
        description = "마지막 대출 가능 여부 확인 결과입니다. AVAILABLE, ON_LOAN, UNAVAILABLE, UNKNOWN, FAILED 중 하나입니다.",
        example = "ON_LOAN",
        nullable = false
    )
    val lastCheckOutcome: String,

    @field:Schema(
        description = "대출 가능 여부를 마지막으로 확인한 시각입니다. 아직 확인하지 않았으면 null입니다.",
        example = "2026-05-07T09:00:01",
        nullable = true
    )
    val lastCheckedAt: LocalDateTime?,

    @field:Schema(
        description = "마지막으로 알림이 생성된 시각입니다. 생성된 알림이 없으면 null입니다.",
        example = "2026-05-07T09:00:01",
        nullable = true
    )
    val lastNotifiedAt: LocalDateTime?
) {
    companion object {
        fun from(result: NotificationSubscriptionResult): NotificationSubscriptionResponse =
            NotificationSubscriptionResponse(
                subscriptionId = result.subscriptionId,
                isbn = result.isbn,
                title = result.title,
                author = result.author,
                coverImageUrl = result.coverImageUrl,
                libraryId = result.libraryId,
                libraryName = result.libraryName,
                lastStableAvailability = result.lastStableAvailability,
                lastCheckOutcome = result.lastCheckOutcome,
                lastCheckedAt = result.lastCheckedAt,
                lastNotifiedAt = result.lastNotifiedAt
            )
    }
}
