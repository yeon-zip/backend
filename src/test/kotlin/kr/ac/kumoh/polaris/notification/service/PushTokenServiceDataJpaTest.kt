package kr.ac.kumoh.polaris.notification.service

import kr.ac.kumoh.polaris.notification.entity.PushPlatform
import kr.ac.kumoh.polaris.notification.repository.UserPushTokenRepository
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.implement.UserReader
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(PushTokenService::class, UserReader::class)
class PushTokenServiceDataJpaTest(
    @Autowired private val pushTokenService: PushTokenService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val userPushTokenRepository: UserPushTokenRepository
) {
    @Test
    fun `token register is idempotent for same user and token`() {
        val user = userRepository.save(testUser("same"))

        pushTokenService.registerToken(user.id!!, PushPlatform.ANDROID, "token-1")
        pushTokenService.registerToken(user.id!!, PushPlatform.ANDROID, "token-1")

        val tokens = userPushTokenRepository.findAll()
        assertEquals(1, tokens.size)
        assertTrue(tokens.single().active)
        assertEquals(user.id, tokens.single().user.id)
    }

    @Test
    fun `token register transfers ownership from previous user`() {
        val firstUser = userRepository.save(testUser("first"))
        val secondUser = userRepository.save(testUser("second"))

        pushTokenService.registerToken(firstUser.id!!, PushPlatform.IOS, "shared-token")
        pushTokenService.registerToken(secondUser.id!!, PushPlatform.IOS, "shared-token")

        val token = userPushTokenRepository.findByPlatformAndDeviceToken(PushPlatform.IOS, "shared-token")!!
        assertEquals(secondUser.id, token.user.id)
        assertTrue(token.active)
    }

    @Test
    fun `token delete is idempotent and soft deactivates owned active token`() {
        val user = userRepository.save(testUser("delete"))

        pushTokenService.deactivateToken(user.id!!, PushPlatform.WEB, "missing")
        pushTokenService.registerToken(user.id!!, PushPlatform.WEB, "delete-token")
        pushTokenService.deactivateToken(user.id!!, PushPlatform.WEB, "delete-token")
        pushTokenService.deactivateToken(user.id!!, PushPlatform.WEB, "delete-token")

        val token = userPushTokenRepository.findByPlatformAndDeviceToken(PushPlatform.WEB, "delete-token")!!
        assertFalse(token.active)
        assertEquals("USER_DELETED", token.deactivationReason)
    }

    private fun testUser(suffix: String): User = User(
        provider = UserAuthProvider.KAKAO,
        oidcIssuer = "https://kauth.kakao.com",
        oidcSubject = "push-$suffix",
        nickname = "push-$suffix"
    )
}
