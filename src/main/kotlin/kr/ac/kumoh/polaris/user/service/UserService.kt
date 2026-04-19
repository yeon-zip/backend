package kr.ac.kumoh.polaris.user.service

import kr.ac.kumoh.polaris.user.implement.UserReader
import kr.ac.kumoh.polaris.user.implement.dto.CurrentUserResult
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userReader: UserReader
) {
    fun getCurrentUser(userId: Long): CurrentUserResult =
        CurrentUserResult.from(userReader.findByIdOrThrow(userId))
}
