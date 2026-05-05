package kr.ac.kumoh.polaris.notification.implement

interface NotificationSender {
    fun sendBookAvailable(
        tokens: List<String>,
        payload: BookAvailableNotificationPayload
    ): NotificationSendResult
}

data class BookAvailableNotificationPayload(
    val isbn: String,
    val libraryId: Long,
    val subscriptionId: Long
)

data class NotificationSendResult(
    val successes: Set<String>,
    val permanentFailures: Set<String>,
    val transientFailures: Set<String>,
    val totalFailure: Boolean = false
) {
    val hasSuccess: Boolean
        get() = successes.isNotEmpty()

    companion object {
        fun totalFailure(tokens: Collection<String>): NotificationSendResult =
            NotificationSendResult(
                successes = emptySet(),
                permanentFailures = emptySet(),
                transientFailures = tokens.toSet(),
                totalFailure = true
            )
    }
}
