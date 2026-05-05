package kr.ac.kumoh.polaris.notification.scheduler

import kr.ac.kumoh.polaris.notification.config.NotificationProperties
import kr.ac.kumoh.polaris.notification.service.NotificationDispatchService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class NotificationDispatchScheduler(
    private val notificationProperties: NotificationProperties,
    private val notificationDispatchService: NotificationDispatchService
) {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val running = AtomicBoolean(false)

    @Scheduled(cron = "\${core.notification.dispatch.cron}", zone = "\${core.notification.dispatch.zone}")
    fun dispatch() {
        if (!notificationProperties.dispatch.enabled) {
            log.debug("Notification dispatch scheduler is disabled")
            return
        }

        if (!running.compareAndSet(false, true)) {
            log.warn("Notification dispatch is already running; skip overlapping execution")
            return
        }

        try {
            val result = notificationDispatchService.dispatchOnce()
            log.info("Notification dispatch completed - {}", result)
        } catch (exception: Exception) {
            log.error("Notification dispatch failed", exception)
        } finally {
            running.set(false)
        }
        // TODO: use a DB/distributed lock before enabling multi-instance scheduled dispatch.
    }
}
