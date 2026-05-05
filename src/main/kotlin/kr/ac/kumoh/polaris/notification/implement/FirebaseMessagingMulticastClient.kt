package kr.ac.kumoh.polaris.notification.implement

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnBean(FirebaseMessaging::class)
class FirebaseMessagingMulticastClient(
    private val firebaseMessaging: FirebaseMessaging
) : FcmMulticastClient {
    override fun sendMulticast(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>
    ): FcmMulticastResponse {
        val message = MulticastMessage.builder()
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .putAllData(data)
            .addAllTokens(tokens)
            .build()

        val response = firebaseMessaging.sendEachForMulticast(message)
        return FcmMulticastResponse(
            results = response.responses.map { sendResponse ->
                if (sendResponse.isSuccessful) {
                    FcmTokenSendResult(success = true)
                } else {
                    FcmTokenSendResult(
                        success = false,
                        failureType = if (isPermanentFailure(sendResponse.exception)) {
                            FcmFailureType.PERMANENT
                        } else {
                            FcmFailureType.TRANSIENT
                        }
                    )
                }
            }
        )
    }

    private fun isPermanentFailure(exception: FirebaseMessagingException?): Boolean =
        when (exception?.messagingErrorCode) {
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT -> true
            else -> false
        }
}
