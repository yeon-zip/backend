package kr.ac.kumoh.polaris.auth.presentation.request

import jakarta.validation.constraints.NotBlank

data class ExchangeCodeRequest(
    @field:NotBlank
    val code: String,
    @field:NotBlank
    val targetId: String,
    val codeVerifier: String? = null
)
