package kr.ac.kumoh.polaris.user.implement

import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class UserReader(
    private val userRepository: UserRepository
) {
    fun findByIdOrThrow(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow {
                ServiceException(
                    errorCode = ErrorCode.USER_NOT_FOUND,
                    message = "사용자를 찾을 수 없습니다. userId=$userId"
                )
            }
}
