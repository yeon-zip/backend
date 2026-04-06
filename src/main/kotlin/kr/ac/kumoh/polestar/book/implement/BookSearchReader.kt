package kr.ac.kumoh.polestar.book.implement

import kr.ac.kumoh.polestar.book.implement.client.NaverSearchBookClient
import kr.ac.kumoh.polestar.book.implement.client.dto.NaverBookSearchItem
import kr.ac.kumoh.polestar.book.implement.dto.BookSearchItemResult
import kr.ac.kumoh.polestar.global.dto.CursorPageResult
import kr.ac.kumoh.polestar.global.exception.ErrorCode
import kr.ac.kumoh.polestar.global.exception.ServiceException
import kr.ac.kumoh.polestar.global.util.IsbnNormalizer
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Base64

@Component
class BookSearchReader(
    private val naverSearchBookClient: NaverSearchBookClient
) {
    /**
     * Cursor v1 is provider-backed and should be reused only with the same query and sort.
     */
    fun searchBooks(
        query: String,
        cursor: String?,
        limit: Int,
        sort: String
    ): CursorPageResult<BookSearchItemResult> {
        val normalizedQuery = normalizeQuery(query)
        val normalizedSort = normalizeSort(sort)

        validateQuery(normalizedQuery)
        validateLimit(limit)
        validateSort(normalizedSort)

        val start = cursor?.let(::decodeCursor) ?: NAVER_START_MIN

        return try {
            val response = naverSearchBookClient.searchBooks(
                query = normalizedQuery,
                display = limit,
                start = start,
                sort = normalizedSort
            )

            val items = response.items.mapNotNull(::toResult)
            val providerNextStart = start + limit
            val hasNext = response.items.size == limit &&
                providerNextStart <= NAVER_START_MAX &&
                providerNextStart <= response.total

            CursorPageResult(
                nextCursor = if (hasNext) encodeCursor(providerNextStart) else null,
                hasNext = hasNext,
                items = items
            )
        } catch (e: ServiceException) {
            throw e
        } catch (e: Exception) {
            throw ServiceException(
                errorCode = ErrorCode.NAVER_SEARCH_FAILED,
                message = "네이버 책 검색 결과 처리에 실패했습니다. query=$normalizedQuery, start=$start, limit=$limit, sort=$normalizedSort"
            )
        }
    }

    private fun normalizeQuery(query: String): String = query.trim()

    private fun normalizeSort(sort: String): String = sort.trim().lowercase()

    private fun validateQuery(query: String) {
        if (query.isBlank()) {
            throw ServiceException(ErrorCode.INVALID_QUERY)
        }
    }

    private fun validateLimit(limit: Int) {
        if (limit !in 1..NAVER_DISPLAY_MAX) {
            throw ServiceException(ErrorCode.INVALID_LIMIT)
        }
    }

    private fun validateSort(sort: String) {
        if (sort !in supportedSorts) {
            throw ServiceException(ErrorCode.INVALID_SORT)
        }
    }

    private fun decodeCursor(cursor: String): Int {
        val decoded = try {
            String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            throw ServiceException(ErrorCode.INVALID_CURSOR)
        }

        val start = decoded.toIntOrNull()
            ?: throw ServiceException(ErrorCode.INVALID_CURSOR)

        if (start !in NAVER_START_MIN..NAVER_START_MAX) {
            throw ServiceException(ErrorCode.INVALID_CURSOR)
        }

        return start
    }

    private fun encodeCursor(start: Int): String =
        Base64.getEncoder().encodeToString(start.toString().toByteArray(StandardCharsets.UTF_8))

    private fun toResult(item: NaverBookSearchItem): BookSearchItemResult? {
        val isbn = extractIsbn13(item.isbn) ?: return null

        return BookSearchItemResult(
            isbn = isbn,
            title = sanitize(item.title).orEmpty(),
            author = sanitize(item.author),
            publisher = sanitize(item.publisher),
            description = sanitize(item.description),
            publicationDate = parsePublicationDate(item.pubdate),
            coverImageUrl = item.image?.trim()?.ifBlank { null },
            link = item.link?.trim()?.ifBlank { null }
        )
    }

    private fun sanitize(raw: String?): String? =
        raw?.replace(htmlTagRegex, "")?.trim()?.ifBlank { null }

    private fun parsePublicationDate(raw: String?): LocalDate? {
        val value = raw?.trim()?.takeIf { it.matches(publicationDateRegex) } ?: return null

        return try {
            LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun extractIsbn13(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val candidate = value.split(whitespaceRegex)
            .firstOrNull { it.matches(isbn13Regex) }
            ?: isbn13Regex.find(value)?.value
            ?: return null

        return runCatching { IsbnNormalizer.normalize(candidate) }.getOrNull()
    }

    companion object {
        private const val NAVER_START_MIN = 1
        private const val NAVER_START_MAX = 1000
        private const val NAVER_DISPLAY_MAX = 100
        private val supportedSorts = setOf("sim", "date")
        private val htmlTagRegex = Regex("<[^>]*>")
        private val publicationDateRegex = Regex("\\d{8}")
        private val whitespaceRegex = Regex("\\s+")
        private val isbn13Regex = Regex("\\d{13}")
    }
}
