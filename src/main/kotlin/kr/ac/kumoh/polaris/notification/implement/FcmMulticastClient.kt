package kr.ac.kumoh.polaris.notification.implement

interface FcmMulticastClient {
    fun sendMulticast(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>
    ): FcmMulticastResponse
}

data class FcmMulticastResponse(
    val results: List<FcmTokenSendResult>
)

data class FcmTokenSendResult(
    val success: Boolean,
    val failureType: FcmFailureType? = null
)

enum class FcmFailureType {
    PERMANENT,
    TRANSIENT
}
