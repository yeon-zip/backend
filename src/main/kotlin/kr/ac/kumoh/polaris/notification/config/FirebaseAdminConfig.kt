package kr.ac.kumoh.polaris.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kr.ac.kumoh.polaris.notification.implement.FcmMulticastClient
import kr.ac.kumoh.polaris.notification.implement.FirebaseMessagingMulticastClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
@ConditionalOnProperty(prefix = "core.notification.fcm", name = ["enabled"], havingValue = "true")
class FirebaseAdminConfig(
    private val notificationProperties: NotificationProperties
) {
    @Bean
    fun firebaseApp(): FirebaseApp {
        FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }?.let { return it }

        val credentials = loadCredentials(notificationProperties.fcm)
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging =
        FirebaseMessaging.getInstance(firebaseApp)

    @Bean
    fun fcmMulticastClient(firebaseMessaging: FirebaseMessaging): FcmMulticastClient =
        FirebaseMessagingMulticastClient(firebaseMessaging)

    private fun loadCredentials(properties: NotificationProperties.Fcm): GoogleCredentials {
        val serviceAccountFile = properties.serviceAccountFile?.takeIf { it.isNotBlank() }
        return when {
            serviceAccountFile != null -> FileInputStream(serviceAccountFile).use(GoogleCredentials::fromStream)
            properties.useAdc -> GoogleCredentials.getApplicationDefault()
            else -> error("notification.fcm.enabled=true requires service-account-file or use-adc=true")
        }
    }
}
