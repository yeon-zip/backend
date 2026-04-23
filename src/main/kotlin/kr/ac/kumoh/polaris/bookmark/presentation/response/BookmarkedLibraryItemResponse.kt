package kr.ac.kumoh.polaris.bookmark.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.ac.kumoh.polaris.bookmark.service.BookmarkedLibraryResult

data class BookmarkedLibraryItemResponse(
    @Schema(description = "도서관 ID입니다.", example = "1", nullable = false)
    val libraryId: Long,
    @Schema(description = "도서관명입니다.", example = "구미시립양포도서관", nullable = false)
    val name: String,
    @Schema(description = "도서관 주소입니다.", example = "경상북도 구미시 옥계북로 51", nullable = false)
    val address: String
) {
    companion object {
        fun from(result: BookmarkedLibraryResult): BookmarkedLibraryItemResponse =
            BookmarkedLibraryItemResponse(
                libraryId = result.libraryId,
                name = result.name,
                address = result.address
            )
    }
}
