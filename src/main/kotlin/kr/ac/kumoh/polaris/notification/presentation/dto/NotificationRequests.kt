package kr.ac.kumoh.polaris.notification.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import kr.ac.kumoh.polaris.notification.entity.PushPlatform

data class RegisterPushTokenRequest(
    @field:NotNull
    val platform: PushPlatform?,
    @field:NotBlank
    val deviceToken: String?
)

data class CreateNotificationSubscriptionRequest(
    @field:NotBlank
    val isbn: String?,
    @field:NotNull
    val libraryId: Long?
)
