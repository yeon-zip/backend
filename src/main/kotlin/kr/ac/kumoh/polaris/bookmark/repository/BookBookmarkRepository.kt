package kr.ac.kumoh.polaris.bookmark.repository

import kr.ac.kumoh.polaris.bookmark.entity.BookBookmark
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BookBookmarkRepository : JpaRepository<BookBookmark, Long> {
    fun existsByUserIdAndBookId(userId: Long, bookId: Long): Boolean

    fun findByUserIdAndBookId(userId: Long, bookId: Long): BookBookmark?

    @Query(
        """
        select bookBookmark.book.isbn
        from BookBookmark bookBookmark
        where bookBookmark.user.id = :userId
          and bookBookmark.book.isbn in :isbns
        """
    )
    fun findBookmarkedIsbnsByUserIdAndIsbnIn(
        @Param("userId") userId: Long,
        @Param("isbns") isbns: Collection<String>
    ): List<String>

    @Query(
        """
        select bookBookmark
        from BookBookmark bookBookmark
        join fetch bookBookmark.book
        where bookBookmark.user.id = :userId
        order by bookBookmark.createdAt desc, bookBookmark.id desc
        """
    )
    fun findAllByUserIdOrderByCreatedAtDesc(@Param("userId") userId: Long): List<BookBookmark>
}
