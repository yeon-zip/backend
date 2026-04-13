package kr.ac.kumoh.polaris.global.util

import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException

object IsbnNormalizer {
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
}
