package kr.ac.kumoh.polaris.book.implement.dto

import kr.ac.kumoh.polaris.bookvote.entity.BookVoteType
import java.time.LocalDate

data class BookResult(
    val isbn: String,
    val title: String,
    val author: String?,
    val publisher: String?,
    val description: String?,
    val publicationDate: LocalDate?,
    val coverImageUrl: String?,
    val isBookmarked: Boolean,
    val recommendCount: Long = 0,
    val notRecommendCount: Long = 0,
    val myVote: BookVoteType? = null
)
