package kr.ac.kumoh.polaris.global.util

import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException

object IsbnNormalizer {
    private val whitespaceRegex = Regex("\\s+")
    private val isbn13Regex = Regex("\\d{13}")
    private val isbn13WithSeparatorsRegex = Regex("97[89](?:[-\\s]?\\d){10}")

    fun normalize(isbn: String): String {
        val normalizedIsbn = isbn.trim().replace("-", "")

        if (!normalizedIsbn.matches(Regex("\\d{13}"))) {
            throw ServiceException(
                errorCode = ErrorCode.INVALID_INPUT_VALUE,
                message = "ISBN은 13자리여야 해요. 입력 값을 다시 확인해 주세요."
            )
        }

        return normalizedIsbn
    }

    fun extractIsbn13(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val separatedCandidate = isbn13WithSeparatorsRegex.find(value)
            ?.value
            ?.replace(Regex("[-\\s]"), "")
        if (separatedCandidate != null) {
            return runCatching { normalize(separatedCandidate) }.getOrNull()
        }

        val sanitized = value.replace(Regex("[^0-9]"), " ")
            .trim()
            .replace(whitespaceRegex, " ")

        val candidate = sanitized.split(whitespaceRegex)
            .firstOrNull { it.matches(isbn13Regex) }
            ?: isbn13Regex.find(sanitized)?.value
            ?: return null

        return runCatching { normalize(candidate) }.getOrNull()
    }
}
