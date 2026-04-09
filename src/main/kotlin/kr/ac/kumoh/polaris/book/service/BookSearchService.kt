package kr.ac.kumoh.polaris.book.service

import kr.ac.kumoh.polaris.book.implement.BookSearchReader
import kr.ac.kumoh.polaris.book.implement.dto.BookSearchItemResult
import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import org.springframework.stereotype.Service

@Service
class BookSearchService(
    private val bookSearchReader: BookSearchReader
) {
    fun searchBooks(
        query: String,
        cursor: String?,
        limit: Int,
        sort: String
    ): CursorPageResult<BookSearchItemResult> =
        bookSearchReader.searchBooks(
            query = query,
            cursor = cursor,
            limit = limit,
            sort = sort
        )
}
