package kr.ac.kumoh.polaris.global.util

import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException

object IsbnNormalizer {
    fun normalize(rawIsbn: String): String {
        val normalized = rawIsbn.trim().replace("-", "")
        if (!normalized.matches(Regex("\\d{13}"))) {
            throw ServiceException(ErrorCode.INVALID_ISBN)
        }
        return normalized
    }
}
