package kr.ac.kumoh.polaris.bookvote.repository

import kr.ac.kumoh.polaris.bookvote.entity.BookVote
import kr.ac.kumoh.polaris.bookvote.entity.BookVoteType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BookVoteRepository : JpaRepository<BookVote, Long> {
    fun findByUserIdAndBookId(userId: Long, bookId: Long): BookVote?

    @Query(
        """
        select new kr.ac.kumoh.polaris.bookvote.repository.BookVoteCountQueryResult(
            book.isbn,
            sum(case when bookVote.voteType = kr.ac.kumoh.polaris.bookvote.entity.BookVoteType.RECOMMEND then 1 else 0 end),
            sum(case when bookVote.voteType = kr.ac.kumoh.polaris.bookvote.entity.BookVoteType.NOT_RECOMMEND then 1 else 0 end)
        )
        from BookVote bookVote
        join bookVote.book book
        where book.isbn in :isbns
        group by book.isbn
        """
    )
    fun findVoteCountsByBookIsbnIn(
        @Param("isbns") isbns: Collection<String>
    ): List<BookVoteCountQueryResult>

    @Query(
        """
        select new kr.ac.kumoh.polaris.bookvote.repository.BookUserVoteQueryResult(
            book.isbn,
            bookVote.voteType
        )
        from BookVote bookVote
        join bookVote.book book
        where bookVote.user.id = :userId
          and book.isbn in :isbns
        """
    )
    fun findUserVotesByUserIdAndBookIsbnIn(
        @Param("userId") userId: Long,
        @Param("isbns") isbns: Collection<String>
    ): List<BookUserVoteQueryResult>
}

data class BookVoteCountQueryResult(
    val isbn: String,
    val recommendCount: Long,
    val notRecommendCount: Long
)

data class BookUserVoteQueryResult(
    val isbn: String,
    val voteType: BookVoteType
)
