package kr.ac.kumoh.polaris.notification.implement

import org.slf4j.LoggerFactory

class FcmNotificationSender(
    private val fcmMulticastClient: FcmMulticastClient
) : NotificationSender {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun sendBookAvailable(
        tokens: List<String>,
        payload: BookAvailableNotificationPayload
    ): NotificationSendResult {
        val normalizedTokens = tokens.map(String::trim).filter(String::isNotBlank).distinct()
        if (normalizedTokens.isEmpty()) {
            return NotificationSendResult(emptySet(), emptySet(), emptySet())
        }

        val successes = linkedSetOf<String>()
        val permanentFailures = linkedSetOf<String>()
        val transientFailures = linkedSetOf<String>()
        var totalCallFailed = false

        for (chunk in normalizedTokens.chunked(MAX_TOKENS_PER_MULTICAST)) {
            try {
                val response = fcmMulticastClient.sendMulticast(
                    tokens = chunk,
                    title = TITLE,
                    body = BODY,
                    data = mapOf(
                        "type" to "BOOK_LIBRARY_AVAILABLE",
                        "isbn" to payload.isbn,
                        "libraryId" to payload.libraryId.toString(),
                        "subscriptionId" to payload.subscriptionId.toString()
                    )
                )
                response.results.forEachIndexed { index, result ->
                    val token = chunk[index]
                    when {
                        result.success -> successes += token
                        result.failureType == FcmFailureType.PERMANENT -> permanentFailures += token
                        else -> transientFailures += token
                    }
                }
            } catch (exception: Exception) {
                totalCallFailed = true
                transientFailures += chunk
                log.error("FCM multicast call failed; preserving all token and subscription state", exception)
            }
        }

        return NotificationSendResult(
            successes = successes,
            permanentFailures = permanentFailures,
            transientFailures = transientFailures,
            totalFailure = totalCallFailed
        )
    }

    companion object {
        const val MAX_TOKENS_PER_MULTICAST = 500
        private const val TITLE = "대출 가능 알림"
        private const val BODY = "구독한 도서가 해당 도서관에서 대출 가능해졌습니다."
    }
}
