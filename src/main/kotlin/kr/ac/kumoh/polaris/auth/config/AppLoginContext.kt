package kr.ac.kumoh.polaris.auth.config

import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import java.io.Serializable
import java.time.LocalDateTime

data class AppLoginContext(
    val channel: AppLoginChannel,
    val targetId: String? = null,
    val codeChallenge: String? = null,
    val codeChallengeMethod: String? = null,
    val requestedAt: LocalDateTime = LocalDateTime.now(),
    val correlationId: String
) : Serializable

enum class AppLoginChannel {
    APP,
    WEB;

    companion object {
        fun from(value: String?): AppLoginChannel = when (value?.trim()?.lowercase()) {
            null, "", "web" -> WEB
            "app" -> APP
            else -> throw ServiceException(
                ErrorCode.OIDC_INVALID_CHANNEL,
                "지원하지 않는 로그인 채널입니다: $value"
            )
        }
    }
}
