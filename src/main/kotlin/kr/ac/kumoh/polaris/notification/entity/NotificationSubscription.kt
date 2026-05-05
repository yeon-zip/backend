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
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification_subscription",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_notification_subscription_user_book_library", columnNames = ["user_id", "book_id", "library_id"])
    ],
    indexes = [
        Index(name = "idx_notification_subscription_active_id", columnList = "active, id"),
        Index(name = "idx_notification_subscription_user_active", columnList = "user_id, active")
    ]
)
class NotificationSubscription(
    user: User,
    book: Book,
    library: Library
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
    @JoinColumn(name = "book_id", nullable = false)
    var book: Book = book
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    var library: Library = library
        protected set

    @Column(name = "active", nullable = false)
    var active: Boolean = true
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "last_stable_availability", nullable = false, length = 30)
    var lastStableAvailability: AlertAvailabilityState = AlertAvailabilityState.UNKNOWN
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "last_check_outcome", nullable = false, length = 30)
    var lastCheckOutcome: AlertCheckOutcome = AlertCheckOutcome.UNKNOWN
        protected set

    @Column(name = "last_checked_at")
    var lastCheckedAt: LocalDateTime? = null
        protected set

    @Column(name = "last_notified_at")
    var lastNotifiedAt: LocalDateTime? = null
        protected set

    @Column(name = "last_dispatch_error_at")
    var lastDispatchErrorAt: LocalDateTime? = null
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    fun reactivate(now: LocalDateTime = LocalDateTime.now()) {
        active = true
        lastStableAvailability = AlertAvailabilityState.UNKNOWN
        lastCheckOutcome = AlertCheckOutcome.UNKNOWN
        lastCheckedAt = null
        lastNotifiedAt = null
        lastDispatchErrorAt = null
        updatedAt = now
    }

    fun deactivate(now: LocalDateTime = LocalDateTime.now()) {
        if (!active) {
            return
        }
        active = false
        updatedAt = now
    }

    fun recordUnknown(now: LocalDateTime) {
        lastCheckOutcome = AlertCheckOutcome.UNKNOWN
        lastCheckedAt = now
        updatedAt = now
    }

    fun recordNotAvailable(outcome: AlertCheckOutcome, now: LocalDateTime) {
        lastStableAvailability = AlertAvailabilityState.NOT_AVAILABLE
        lastCheckOutcome = outcome
        lastCheckedAt = now
        lastDispatchErrorAt = null
        updatedAt = now
    }

    fun recordAvailableNotified(now: LocalDateTime) {
        lastStableAvailability = AlertAvailabilityState.AVAILABLE
        lastCheckOutcome = AlertCheckOutcome.AVAILABLE
        lastCheckedAt = now
        lastNotifiedAt = now
        lastDispatchErrorAt = null
        updatedAt = now
    }

    fun recordAvailableObserved(now: LocalDateTime) {
        lastStableAvailability = AlertAvailabilityState.AVAILABLE
        lastCheckOutcome = AlertCheckOutcome.AVAILABLE
        lastCheckedAt = now
        lastDispatchErrorAt = null
        updatedAt = now
    }

    fun recordDispatchFailure(now: LocalDateTime) {
        lastCheckOutcome = AlertCheckOutcome.AVAILABLE
        lastCheckedAt = now
        lastDispatchErrorAt = now
        updatedAt = now
    }

    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
