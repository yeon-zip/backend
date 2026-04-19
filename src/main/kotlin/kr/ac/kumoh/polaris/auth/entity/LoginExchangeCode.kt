package kr.ac.kumoh.polaris.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import kr.ac.kumoh.polaris.user.entity.User
import java.time.LocalDateTime

@Entity
@Table(name = "login_exchange_code")
class LoginExchangeCode(
    codeHash: String,
    targetId: String,
    codeChallengeHash: String?,
    expiresAt: LocalDateTime,
    correlationId: String,
    user: User
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "code_hash", nullable = false, unique = true, length = 64)
    var codeHash: String = codeHash
        protected set

    @Column(name = "target_id", nullable = false, length = 100)
    var targetId: String = targetId
        protected set

    @Column(name = "code_challenge_hash", length = 64)
    var codeChallengeHash: String? = codeChallengeHash
        protected set

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime = expiresAt
        protected set

    @Column(name = "consumed_at")
    var consumedAt: LocalDateTime? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "issuance_status", nullable = false, length = 30)
    var issuanceStatus: LoginExchangeIssuanceStatus = LoginExchangeIssuanceStatus.PENDING
        protected set

    @Column(name = "issued_refresh_token_id")
    var issuedRefreshTokenId: Long? = null
        protected set

    @Column(name = "failure_reason", length = 200)
    var failureReason: String? = null
        protected set

    @Column(name = "correlation_id", nullable = false, length = 100)
    var correlationId: String = correlationId
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean = expiresAt.isBefore(now)

    fun markIssued(
        consumedAt: LocalDateTime,
        refreshTokenId: Long?
    ) {
        this.consumedAt = consumedAt
        issuanceStatus = LoginExchangeIssuanceStatus.ISSUED
        issuedRefreshTokenId = refreshTokenId
        failureReason = null
    }

    fun markFailed(
        consumedAt: LocalDateTime,
        reason: String
    ) {
        this.consumedAt = consumedAt
        issuanceStatus = LoginExchangeIssuanceStatus.FAILED_TERMINAL
        failureReason = reason
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
