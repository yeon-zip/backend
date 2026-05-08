package kr.ac.kumoh.polaris.notification.repository

import kr.ac.kumoh.polaris.notification.entity.NotificationType
import kr.ac.kumoh.polaris.notification.entity.UserNotification
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface UserNotificationRepository : JpaRepository<UserNotification, Long> {
    fun countByUserIdAndDeletedAtIsNull(userId: Long): Long

    fun existsBySubscriptionIdAndNotificationTypeAndNotificationDate(
        subscriptionId: Long,
        notificationType: NotificationType,
        notificationDate: LocalDate
    ): Boolean

    @Query(
        """
        select notification
        from UserNotification notification
        join fetch notification.book
        join fetch notification.library
        where notification.user.id = :userId
          and notification.deletedAt is null
          and (:cursorId is null or notification.id < :cursorId)
        order by notification.id desc
        """
    )
    fun findVisiblePageByUserId(
        @Param("userId") userId: Long,
        @Param("cursorId") cursorId: Long?,
        pageable: Pageable
    ): List<UserNotification>
}
