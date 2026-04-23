package kr.ac.kumoh.polaris.bookmark.presentation.response

import kr.ac.kumoh.polaris.bookmark.service.BookmarkedLibraryResult

data class BookmarkedLibrariesResponse(
    val items: List<BookmarkedLibraryItemResponse>
) {
    companion object {
        fun from(results: List<BookmarkedLibraryResult>): BookmarkedLibrariesResponse =
            BookmarkedLibrariesResponse(
                items = results.map(BookmarkedLibraryItemResponse::from)
            )
    }
}
