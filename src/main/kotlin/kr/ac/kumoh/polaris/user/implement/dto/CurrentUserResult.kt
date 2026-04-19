package kr.ac.kumoh.polaris.user.implement.dto

import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.entity.UserRole

data class CurrentUserResult(
    val id: Long,
    val provider: UserAuthProvider,
    val role: UserRole,
    val nickname: String?,
    val email: String?,
    val profileImageUrl: String?
) {
    companion object {
        fun from(user: User): CurrentUserResult = CurrentUserResult(
            id = user.id ?: throw IllegalStateException("사용자 ID가 없습니다."),
            provider = user.provider,
            role = user.role,
            nickname = user.nickname,
            email = user.email,
            profileImageUrl = user.profileImageUrl
        )
    }
}
