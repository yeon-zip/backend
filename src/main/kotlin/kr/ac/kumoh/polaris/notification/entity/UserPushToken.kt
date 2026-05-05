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
import kr.ac.kumoh.polaris.user.entity.User
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_push_token",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_push_token_platform_device", columnNames = ["platform", "device_token"])
    ],
    indexes = [
        Index(name = "idx_user_push_token_user_active", columnList = "user_id, active")
    ]
)
class UserPushToken(
    user: User,
    platform: PushPlatform,
    deviceToken: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 30)
    var platform: PushPlatform = platform
        protected set

    @Column(name = "device_token", nullable = false, length = 500)
    var deviceToken: String = deviceToken
        protected set

    @Column(name = "active", nullable = false)
    var active: Boolean = true
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "last_confirmed_at", nullable = false)
    var lastConfirmedAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "deactivated_at")
    var deactivatedAt: LocalDateTime? = null
        protected set

    @Column(name = "deactivation_reason", length = 100)
    var deactivationReason: String? = null
        protected set

    fun registerTo(user: User, now: LocalDateTime = LocalDateTime.now()) {
        this.user = user
        this.active = true
        this.lastConfirmedAt = now
        this.deactivatedAt = null
        this.deactivationReason = null
        this.updatedAt = now
    }

    fun deactivate(reason: String, now: LocalDateTime = LocalDateTime.now()) {
        if (!active) {
            return
        }
        active = false
        deactivatedAt = now
        deactivationReason = reason.take(100)
        updatedAt = now
    }

    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
        lastConfirmedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
