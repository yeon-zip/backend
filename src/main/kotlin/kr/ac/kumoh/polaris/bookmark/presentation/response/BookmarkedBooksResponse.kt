package kr.ac.kumoh.polaris.bookmark.presentation.response

import kr.ac.kumoh.polaris.bookmark.service.BookmarkedBookResult

data class BookmarkedBooksResponse(
    val items: List<BookmarkedBookItemResponse>
) {
    companion object {
        fun from(results: List<BookmarkedBookResult>): BookmarkedBooksResponse =
            BookmarkedBooksResponse(
                items = results.map(BookmarkedBookItemResponse::from)
            )
    }
}
