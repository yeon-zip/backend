package kr.ac.kumoh.polaris.bookvote.implement

import kr.ac.kumoh.polaris.bookvote.implement.dto.BookVoteSummaryResult
import kr.ac.kumoh.polaris.bookvote.repository.BookVoteRepository
import org.springframework.stereotype.Component

@Component
class BookVoteSummaryReader(
    private val bookVoteRepository: BookVoteRepository
) {
    fun getBookVoteSummary(
        userId: Long?,
        isbn: String
    ): BookVoteSummaryResult =
        getBookVoteSummaries(userId, listOf(isbn))[isbn] ?: BookVoteSummaryResult.empty()

    fun getBookVoteSummaries(
        userId: Long?,
        isbns: Collection<String>
    ): Map<String, BookVoteSummaryResult> {
        if (isbns.isEmpty()) {
            return emptyMap()
        }

        val isbnList = isbns.distinct()
        val countMap = bookVoteRepository.findVoteCountsByBookIsbnIn(isbnList)
            .associateBy { it.isbn }
        val myVotes = if (userId == null) {
            emptyMap()
        } else {
            bookVoteRepository.findUserVotesByUserIdAndBookIsbnIn(userId, isbnList)
                .associate { it.isbn to it.voteType }
        }

        return isbnList.associateWith { isbn ->
            val count = countMap[isbn]
            BookVoteSummaryResult(
                recommendCount = count?.recommendCount ?: 0,
                notRecommendCount = count?.notRecommendCount ?: 0,
                myVote = myVotes[isbn]
            )
        }
    }
}
