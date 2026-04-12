package kr.ac.kumoh.polaris.book.implement

import kr.ac.kumoh.polaris.book.implement.client.NaverSearchBookDetailClient
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Component
class BookMetadataLoader(
    private val naverSearchBookDetailClient: NaverSearchBookDetailClient
) {
    companion object {
        private val PUBLICATION_DATE_REGEX = Regex("\\d{8}")
    }

    fun loadByIsbn(isbn: String): BookMetadata? =
        naverSearchBookDetailClient.searchBookDetails(isbn = isbn, display = 1)
            .items
            .firstOrNull()
            ?.let { detail ->
                val title = detail.title?.takeIf { it.isNotBlank() } ?: return null
                val resolvedIsbn = detail.isbn?.takeIf { it.isNotBlank() } ?: isbn

                BookMetadata(
                    isbn = resolvedIsbn,
                    title = title,
                    author = detail.author?.takeIf { it.isNotBlank() },
                    publisher = detail.publisher?.takeIf { it.isNotBlank() },
                    description = detail.description?.takeIf { it.isNotBlank() },
                    publicationDate = parsePublicationDate(detail.pubdate),
                    coverImageUrl = detail.image?.takeIf { it.isNotBlank() }
                )
            }

    private fun parsePublicationDate(raw: String?): LocalDate? {
        val value = raw?.trim()?.takeIf { it.matches(PUBLICATION_DATE_REGEX) } ?: return null

        return try {
            LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (_: DateTimeParseException) {
            null
        }
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
