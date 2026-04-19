package kr.ac.kumoh.polaris.auth.presentation.response

import kr.ac.kumoh.polaris.user.presentation.response.CurrentUserResponse

data class OidcLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: CurrentUserResponse
)
