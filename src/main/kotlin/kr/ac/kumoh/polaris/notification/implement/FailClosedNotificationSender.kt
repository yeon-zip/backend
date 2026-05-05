package kr.ac.kumoh.polaris.notification.implement

import org.slf4j.LoggerFactory

class FailClosedNotificationSender : NotificationSender {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun sendBookAvailable(
        tokens: List<String>,
        payload: BookAvailableNotificationPayload
    ): NotificationSendResult {
        log.warn("FCM sender is not configured; notification send blocked fail-closed")
        return NotificationSendResult.totalFailure(tokens)
    }
}
