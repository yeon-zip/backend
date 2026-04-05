package kr.ac.kumoh.polestar.book.presentation.response

import kr.ac.kumoh.polestar.book.implement.dto.BookSearchItemResult
import java.time.LocalDate

data class BookSearchItemResponse(
    val isbn: String,
    val title: String,
    val author: String?,
    val publisher: String?,
    val description: String?,
    val publicationDate: LocalDate?,
    val coverImageUrl: String?,
    val link: String?
) {
    companion object {
        fun from(result: BookSearchItemResult): BookSearchItemResponse =
            BookSearchItemResponse(
                isbn = result.isbn,
                title = result.title,
                author = result.author,
                publisher = result.publisher,
                description = result.description,
                publicationDate = result.publicationDate,
                coverImageUrl = result.coverImageUrl,
                link = result.link
            )
    }
}
