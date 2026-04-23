package kr.ac.kumoh.polaris.bookmark.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
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
import kr.ac.kumoh.polaris.user.entity.User
import java.time.LocalDateTime

@Entity
@Table(
    name = "book_bookmark",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_book_bookmark_user_book", columnNames = ["user_id", "book_id"])
    ],
    indexes = [
        Index(name = "idx_book_bookmark_user_created_at", columnList = "user_id, created_at"),
        Index(name = "idx_book_bookmark_book_id", columnList = "book_id")
    ]
)
class BookBookmark(
    user: User,
    book: Book
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

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

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
