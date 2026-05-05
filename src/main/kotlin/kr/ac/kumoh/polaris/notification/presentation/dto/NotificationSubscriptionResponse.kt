package kr.ac.kumoh.polaris.notification.presentation.dto

import kr.ac.kumoh.polaris.notification.service.NotificationSubscriptionResult
import java.time.LocalDateTime

data class NotificationSubscriptionResponse(
    val subscriptionId: Long,
    val isbn: String,
    val title: String,
    val author: String?,
    val coverImageUrl: String?,
    val libraryId: Long,
    val libraryName: String,
    val lastStableAvailability: String,
    val lastCheckOutcome: String,
    val lastCheckedAt: LocalDateTime?,
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
