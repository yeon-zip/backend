package kr.ac.kumoh.polaris.auth.principal

import kr.ac.kumoh.polaris.user.entity.UserRole

data class AuthenticatedUser(
    val id: Long,
    val role: UserRole
)
