package kr.ac.kumoh.polestar.global.util

import kr.ac.kumoh.polestar.global.exception.ErrorCode
import kr.ac.kumoh.polestar.global.exception.ServiceException

object IsbnNormalizer {
    fun normalize(rawIsbn: String): String {
        val normalized = rawIsbn.trim().replace("-", "")
        if (!normalized.matches(Regex("\\d{13}"))) {
            throw ServiceException(ErrorCode.INVALID_ISBN)
        }
        return normalized
    }
}
