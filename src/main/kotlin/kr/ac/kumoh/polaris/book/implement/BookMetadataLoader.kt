package kr.ac.kumoh.polaris.book.implement

import kr.ac.kumoh.polaris.book.implement.client.NationalLibraryBookClient
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BookMetadataLoader(
    private val nationalLibraryBookClient: NationalLibraryBookClient
) {
    fun loadByIsbn(isbn: String): BookMetadata? =
        nationalLibraryBookClient.fetchByIsbn(isbn)
            ?.let { detail ->
                BookMetadata(
                    isbn = detail.isbn,
                    title = detail.title,
                    author = detail.author,
                    publisher = detail.publisher,
                    description = detail.description,
                    publicationDate = detail.publicationDate,
                    coverImageUrl = detail.coverImageUrl
                )
            }
}

data class BookMetadata(
    val isbn: String,
    val title: String,
    val author: String?,
    val publisher: String?,
    val description: String?,
    val publicationDate: LocalDate?,
    val coverImageUrl: String?
)
