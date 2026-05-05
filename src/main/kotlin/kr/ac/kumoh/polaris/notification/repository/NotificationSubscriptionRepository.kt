package kr.ac.kumoh.polaris.notification.repository

import kr.ac.kumoh.polaris.notification.entity.NotificationSubscription
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationSubscriptionRepository : JpaRepository<NotificationSubscription, Long> {
    fun findByUserIdAndBookIdAndLibraryId(
        userId: Long,
        bookId: Long,
        libraryId: Long
    ): NotificationSubscription?

    @Query(
        """
        select subscription
        from NotificationSubscription subscription
        join fetch subscription.book
        join fetch subscription.library
        where subscription.user.id = :userId
          and subscription.active = true
        order by subscription.createdAt desc, subscription.id desc
        """
    )
    fun findActiveByUserId(@Param("userId") userId: Long): List<NotificationSubscription>

    @Query(
        """
        select subscription
        from NotificationSubscription subscription
        join fetch subscription.user
        join fetch subscription.book
        join fetch subscription.library
        where subscription.active = true
          and subscription.id > :afterId
        order by subscription.id asc
        """
    )
    fun findActivePageAfterId(
        @Param("afterId") afterId: Long,
        pageable: Pageable
    ): List<NotificationSubscription>
}
