package kr.ac.kumoh.polaris.notification.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.library.entity.Library
import kr.ac.kumoh.polaris.user.entity.User
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_notification",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_notification_subscription_type_date",
            columnNames = ["subscription_id", "notification_type", "notification_date"]
        )
    ],
    indexes = [
        Index(name = "idx_user_notification_user_deleted_id", columnList = "user_id, deleted_at, id")
    ]
)
class UserNotification(
    user: User,
    subscription: NotificationSubscription,
    book: Book,
    library: Library,
    notificationType: NotificationType,
    title: String,
    message: String,
    notificationDate: LocalDate,
    createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    var subscription: NotificationSubscription = subscription
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    var book: Book = book
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    var library: Library = library
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    var notificationType: NotificationType = notificationType
        protected set

    @Column(name = "title", nullable = false, length = 100)
    var title: String = title
        protected set

    @Column(name = "message", nullable = false, length = 500)
    var message: String = message
        protected set

    @Column(name = "notification_date", nullable = false)
    var notificationDate: LocalDate = notificationDate
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = createdAt
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = createdAt
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun softDelete(now: LocalDateTime = LocalDateTime.now()) {
        if (deletedAt != null) return
        deletedAt = now
        updatedAt = now
    }

    @PrePersist
    fun onCreate() {
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
