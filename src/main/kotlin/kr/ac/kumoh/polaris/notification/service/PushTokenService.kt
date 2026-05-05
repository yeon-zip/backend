package kr.ac.kumoh.polaris.notification.service

import kr.ac.kumoh.polaris.notification.entity.PushPlatform
import kr.ac.kumoh.polaris.notification.entity.UserPushToken
import kr.ac.kumoh.polaris.notification.repository.UserPushTokenRepository
import kr.ac.kumoh.polaris.user.implement.UserReader
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PushTokenService(
    private val userReader: UserReader,
    private val userPushTokenRepository: UserPushTokenRepository
) {
    @Transactional
    fun registerToken(userId: Long, platform: PushPlatform, deviceToken: String) {
        val normalizedToken = normalizeDeviceToken(deviceToken)
        val user = userReader.findByIdOrThrow(userId)

        val existing = userPushTokenRepository.findByPlatformAndDeviceToken(platform, normalizedToken)
        if (existing != null) {
            existing.registerTo(user)
            return
        }

        try {
            userPushTokenRepository.saveAndFlush(
                UserPushToken(
                    user = user,
                    platform = platform,
                    deviceToken = normalizedToken
                )
            )
        } catch (exception: DataIntegrityViolationException) {
            userPushTokenRepository.findByPlatformAndDeviceToken(platform, normalizedToken)
                ?.registerTo(user)
                ?: throw exception
        }
    }

    @Transactional
    fun deactivateToken(userId: Long, platform: PushPlatform, deviceToken: String) {
        val normalizedToken = normalizeDeviceToken(deviceToken)
        userPushTokenRepository.findByUserIdAndPlatformAndDeviceToken(userId, platform, normalizedToken)
            ?.deactivate("USER_DELETED")
    }

    private fun normalizeDeviceToken(deviceToken: String): String = deviceToken.trim()
}
