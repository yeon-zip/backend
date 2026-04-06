package kr.ac.kumoh.polestar.book.service

import kr.ac.kumoh.polestar.book.implement.BookSearchReader
import kr.ac.kumoh.polestar.book.implement.dto.BookSearchItemResult
import kr.ac.kumoh.polestar.global.dto.CursorPageResult
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
