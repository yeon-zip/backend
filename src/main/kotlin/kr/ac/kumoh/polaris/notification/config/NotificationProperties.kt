package kr.ac.kumoh.polaris.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "core.notification")
data class NotificationProperties(
    val dispatch: Dispatch = Dispatch()
) {
    data class Dispatch(
        val enabled: Boolean = false,
        val cron: String = "0 0 9 * * *",
        val zone: String = "Asia/Seoul",
        val batchSize: Int = 100
    )
}
