package kr.ac.kumoh.polaris.book.service

import kr.ac.kumoh.polaris.book.implement.BookSearchReader
import kr.ac.kumoh.polaris.book.implement.dto.BookSearchItemResult
import kr.ac.kumoh.polaris.bookmark.implement.BookmarkStatusReader
import kr.ac.kumoh.polaris.bookvote.implement.BookVoteSummaryReader
import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import org.springframework.stereotype.Service

@Service
class BookSearchService(
    private val bookSearchReader: BookSearchReader,
    private val bookmarkStatusReader: BookmarkStatusReader,
    private val bookVoteSummaryReader: BookVoteSummaryReader
) {
    fun searchBooks(
        query: String,
        cursor: String?,
        limit: Int,
        userId: Long? = null
    ): CursorPageResult<BookSearchItemResult> {
        val result = bookSearchReader.searchBooks(
            query = query,
            cursor = cursor,
            limit = limit
        )

        val bookmarkedIsbns = bookmarkStatusReader.getBookmarkedIsbns(
            userId = userId,
            isbns = result.items.map { it.isbn }
        )
        val voteSummaries = bookVoteSummaryReader.getBookVoteSummaries(
            userId = userId,
            isbns = result.items.map { it.isbn }
        )

        return result.copy(
            items = result.items.map { item ->
                val voteSummary = voteSummaries[item.isbn]
                item.copy(
                    isBookmarked = bookmarkedIsbns.contains(item.isbn),
                    recommendCount = voteSummary?.recommendCount ?: 0,
                    notRecommendCount = voteSummary?.notRecommendCount ?: 0,
                    myVote = voteSummary?.myVote
                )
            }
        )
    }
}
