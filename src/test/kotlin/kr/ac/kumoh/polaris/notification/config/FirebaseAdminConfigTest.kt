package kr.ac.kumoh.polaris.notification.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FirebaseAdminConfigTest {
    @Test
    fun `missing credential with ADC disabled fails closed`() {
        val config = FirebaseAdminConfig(
            NotificationProperties(
                fcm = NotificationProperties.Fcm(
                    enabled = true,
                    serviceAccountFile = null,
                    useAdc = false
                )
            )
        )

        val exception = assertThrows<IllegalStateException> {
            config.firebaseApp()
        }

        assertTrue(exception.message!!.contains("requires service-account-file or use-adc=true"))
    }
}
