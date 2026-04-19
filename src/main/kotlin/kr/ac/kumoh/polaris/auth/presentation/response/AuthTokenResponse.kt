package kr.ac.kumoh.polaris.auth.presentation.response

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val userId: Long
)
