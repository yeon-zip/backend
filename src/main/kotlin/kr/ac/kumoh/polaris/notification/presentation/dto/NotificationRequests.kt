package kr.ac.kumoh.polaris.notification.presentation.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateNotificationSubscriptionRequest(
    @field:NotBlank
    @field:Schema(
        description = "알림 받기 설정할 도서를 식별하는 ISBN입니다. 하이픈이 포함되어도 서버에서 정규화합니다.",
        example = "9791191111111",
        nullable = false
    )
    val isbn: String?,
    @field:NotNull
    @field:Schema(
        description = "알림 받기 설정할 도서관을 식별하는 ID입니다.",
        example = "10",
        nullable = false
    )
    val libraryId: Long?
)
