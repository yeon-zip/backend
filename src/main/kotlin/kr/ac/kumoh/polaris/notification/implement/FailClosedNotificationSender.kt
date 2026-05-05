package kr.ac.kumoh.polaris.notification.implement

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnMissingBean(NotificationSender::class)
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
