package kr.ac.kumoh.polaris.book.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "book")
class Book(
    isbn: String? = null,
    title: String,
    author: String? = null,
    publisher: String? = null,
    description: String? = null,
    price: Int? = null,
    publicationDate: LocalDate? = null,
    coverImageUrl: String? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "isbn", length = 13, unique = true)
    var isbn: String? = isbn
        protected set

    @Column(name = "title", nullable = false, length = 300)
    var title: String = title
        protected set

    @Column(name = "author", length = 300)
    var author: String? = author
        protected set

    @Column(name = "publisher", length = 200)
    var publisher: String? = publisher
        protected set

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = description
        protected set

    @Column(name = "price")
    var price: Int? = price
        protected set

    @Column(name = "publication_date")
    var publicationDate: LocalDate? = publicationDate
        protected set

    @Column(name = "cover_image_url", length = 500)
    var coverImageUrl: String? = coverImageUrl
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set
}
