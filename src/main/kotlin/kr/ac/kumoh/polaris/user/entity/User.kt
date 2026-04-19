package kr.ac.kumoh.polaris.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "app_user",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_app_user_oidc_identity", columnNames = ["oidc_issuer", "oidc_subject"])
    ]
)
class User(
    provider: UserAuthProvider,
    oidcIssuer: String,
    oidcSubject: String,
    nickname: String? = null,
    email: String? = null,
    profileImageUrl: String? = null,
    role: UserRole = UserRole.USER
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    var provider: UserAuthProvider = provider
        protected set

    @Column(name = "oidc_issuer", nullable = false, length = 200)
    var oidcIssuer: String = oidcIssuer
        protected set

    @Column(name = "oidc_subject", nullable = false, length = 200)
    var oidcSubject: String = oidcSubject
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    var role: UserRole = role
        protected set

    @Column(name = "nickname", length = 100)
    var nickname: String? = nickname
        protected set

    @Column(name = "email", length = 255)
    var email: String? = email
        protected set

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = profileImageUrl
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    fun updateProfile(
        nickname: String?,
        email: String?,
        profileImageUrl: String?
    ) {
        this.nickname = nickname
        this.email = email
        this.profileImageUrl = profileImageUrl
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
