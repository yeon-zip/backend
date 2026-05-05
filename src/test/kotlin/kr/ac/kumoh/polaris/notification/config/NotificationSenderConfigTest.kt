package kr.ac.kumoh.polaris.notification.config

import kr.ac.kumoh.polaris.notification.implement.FailClosedNotificationSender
import kr.ac.kumoh.polaris.notification.implement.FcmMulticastClient
import kr.ac.kumoh.polaris.notification.implement.FcmMulticastResponse
import kr.ac.kumoh.polaris.notification.implement.FcmNotificationSender
import kr.ac.kumoh.polaris.notification.implement.NotificationSender
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class NotificationSenderConfigTest {
    @Test
    fun `uses fail closed sender when FCM client is absent`() {
        ApplicationContextRunner()
            .withUserConfiguration(NotificationSenderConfig::class.java)
            .run { context ->
                assertInstanceOf(
                    FailClosedNotificationSender::class.java,
                    context.getBean(NotificationSender::class.java)
                )
            }
    }

    @Test
    fun `uses FCM sender when FCM client is present`() {
        ApplicationContextRunner()
            .withUserConfiguration(
                NotificationSenderConfig::class.java,
                FcmClientTestConfig::class.java
            )
            .run { context ->
                assertInstanceOf(
                    FcmNotificationSender::class.java,
                    context.getBean(NotificationSender::class.java)
                )
            }
    }

    @Configuration(proxyBeanMethods = false)
    private class FcmClientTestConfig {
        @Bean
        fun fcmMulticastClient(): FcmMulticastClient =
            object : FcmMulticastClient {
                override fun sendMulticast(
                    tokens: List<String>,
                    title: String,
                    body: String,
                    data: Map<String, String>
                ): FcmMulticastResponse = FcmMulticastResponse(emptyList())
            }
    }
}
