package kr.ac.kumoh.polaris.user.presentation.response

import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.entity.UserRole
import kr.ac.kumoh.polaris.user.implement.dto.CurrentUserResult

data class CurrentUserResponse(
    val id: Long,
    val provider: UserAuthProvider,
    val role: UserRole,
    val nickname: String?,
    val email: String?,
    val profileImageUrl: String?
) {
    companion object {
        fun from(result: CurrentUserResult): CurrentUserResponse = CurrentUserResponse(
            id = result.id,
            provider = result.provider,
            role = result.role,
            nickname = result.nickname,
            email = result.email,
            profileImageUrl = result.profileImageUrl
        )
    }
}
