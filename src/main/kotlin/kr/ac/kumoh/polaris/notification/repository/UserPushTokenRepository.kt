package kr.ac.kumoh.polaris.notification.repository

import kr.ac.kumoh.polaris.notification.entity.PushPlatform
import kr.ac.kumoh.polaris.notification.entity.UserPushToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserPushTokenRepository : JpaRepository<UserPushToken, Long> {
    fun findByPlatformAndDeviceToken(platform: PushPlatform, deviceToken: String): UserPushToken?

    fun findByUserIdAndPlatformAndDeviceToken(
        userId: Long,
        platform: PushPlatform,
        deviceToken: String
    ): UserPushToken?

    fun findAllByUserIdAndActiveTrue(userId: Long): List<UserPushToken>

    @Query(
        """
        select token
        from UserPushToken token
        join fetch token.user
        where token.user.id in :userIds
          and token.active = true
        """
    )
    fun findActiveByUserIdIn(@Param("userIds") userIds: Collection<Long>): List<UserPushToken>
}
