package kr.ac.kumoh.polaris.book.implement.dto

import kr.ac.kumoh.polaris.book.implement.client.dto.NaverBookSearchItem
import kr.ac.kumoh.polaris.global.util.IsbnNormalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class BookSearchItemResult(
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
        private val htmlTagRegex = Regex("<[^>]*>")
        private val publicationDateRegex = Regex("\\d{8}")
        private val whitespaceRegex = Regex("\\s+")
        private val isbn13Regex = Regex("\\d{13}")

        fun from(item: NaverBookSearchItem): BookSearchItemResult? {
            val isbn = extractIsbn13(item.isbn) ?: return null
            val title = sanitizeHtml(item.title) ?: return null

            return BookSearchItemResult(
                isbn = isbn,
                title = title,
                author = sanitizeHtml(item.author),
                publisher = sanitizeHtml(item.publisher),
                description = sanitizeHtml(item.description),
                publicationDate = parsePublicationDate(item.pubdate),
                coverImageUrl = item.image?.trim()?.ifBlank { null },
                link = item.link?.trim()?.ifBlank { null }
            )
        }

        /**
         * 문자열에서 HTML 태그를 제거하고 앞뒤 공백을 정리합니다.
         *
         * @param raw 원본 문자열
         * @return HTML 태그와 불필요한 공백이 제거된 문자열입니다. 처리할 수 없으면 {@code null}을 반환합니다.
         */
        private fun sanitizeHtml(raw: String?): String? =
            raw?.replace(htmlTagRegex, "")?.trim()?.ifBlank { null }

        /**
         * 네이버 책 검색 API의 출판일 문자열을 {@link LocalDate}로 변환합니다.
         *
         * @param raw 원본 문자열
         * @return 변환된 출판일입니다. 처리할 수 없으면 {@code null}을 반환합니다.
         */
        private fun parsePublicationDate(raw: String?): LocalDate? {
            val value = raw?.trim()?.takeIf { it.matches(publicationDateRegex) } ?: return null

            return try {
                LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
            } catch (_: DateTimeParseException) {
                null
            }
        }

        /**
         * 네이버 책 검색 API의 ISBN 문자열에서 ISBN-13을 추출하고 정규화합니다.
         *
         * @param raw 원본 문자열
         * @return 정규화된 ISBN-13입니다. 처리할 수 없으면 {@code null}을 반환합니다.
         */
        private fun extractIsbn13(raw: String?): String? {
            val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null

            val candidate = value.split(whitespaceRegex)
                .firstOrNull { it.matches(isbn13Regex) }
                ?: isbn13Regex.find(value)?.value
                ?: return null

            return runCatching { IsbnNormalizer.normalize(candidate) }.getOrNull()
        }
    }
}
