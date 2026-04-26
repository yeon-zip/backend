package kr.ac.kumoh.polaris.bookvote.implement.dto

import kr.ac.kumoh.polaris.bookvote.entity.BookVoteType

data class BookVoteSummaryResult(
    val recommendCount: Long,
    val notRecommendCount: Long,
    val myVote: BookVoteType?
) {
    companion object {
        fun empty(): BookVoteSummaryResult = BookVoteSummaryResult(
            recommendCount = 0,
            notRecommendCount = 0,
            myVote = null
        )
    }
}
