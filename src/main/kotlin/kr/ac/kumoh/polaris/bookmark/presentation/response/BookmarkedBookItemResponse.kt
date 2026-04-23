package kr.ac.kumoh.polaris.bookmark.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.ac.kumoh.polaris.bookmark.service.BookmarkedBookResult

data class BookmarkedBookItemResponse(
    @Schema(description = "ISBN입니다. 13자리 길이를 가집니다.", example = "9791198363510", nullable = false)
    val isbn: String,
    @Schema(description = "도서명입니다.", example = "아몬드", nullable = false)
    val title: String,
    @Schema(description = "저자입니다.", example = "손원평", nullable = true)
    val author: String?,
    @Schema(description = "도서 표지 URL입니다.", example = "https://nl.go.kr/seoji/fu/ecip/dbfiles/CIP_FILES_TBL/2023/06/9791198363510.jpg", nullable = true)
    val coverImageUrl: String?
) {
    companion object {
        fun from(result: BookmarkedBookResult): BookmarkedBookItemResponse =
            BookmarkedBookItemResponse(
                isbn = result.isbn,
                title = result.title,
                author = result.author,
                coverImageUrl = result.coverImageUrl
            )
    }
}
