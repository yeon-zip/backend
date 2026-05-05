package kr.ac.kumoh.polaris.notification.config

import kr.ac.kumoh.polaris.notification.implement.FailClosedNotificationSender
import kr.ac.kumoh.polaris.notification.implement.FcmMulticastClient
import kr.ac.kumoh.polaris.notification.implement.FcmNotificationSender
import kr.ac.kumoh.polaris.notification.implement.NotificationSender
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class NotificationSenderConfig {
    @Bean
    @ConditionalOnMissingBean(NotificationSender::class)
    fun notificationSender(
        fcmMulticastClient: ObjectProvider<FcmMulticastClient>
    ): NotificationSender =
        fcmMulticastClient.getIfAvailable()?.let(::FcmNotificationSender)
            ?: FailClosedNotificationSender()
}
