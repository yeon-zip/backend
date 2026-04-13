package kr.ac.kumoh.polaris.book.implement

import kr.ac.kumoh.polaris.book.implement.client.NaverSearchBookListClient
import kr.ac.kumoh.polaris.book.implement.dto.BookSearchItemResult
import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import org.springframework.stereotype.Component

@Component
class BookSearchReader(
    private val naverSearchBookClient: NaverSearchBookListClient
) {
    companion object {
        private const val NAVER_START_MIN = 1
        private const val NAVER_START_MAX = 1000
        private const val NAVER_DISPLAY_MAX = 100
    }

    fun searchBooks(
        query: String,
        cursor: String?,
        limit: Int
    ): CursorPageResult<BookSearchItemResult> {
        val normalizedQuery = query.trim()

        validate(normalizedQuery, limit)

        val start = cursor?.toInt() ?: NAVER_START_MIN

        return try {
            val response = naverSearchBookClient.searchBooks(
                query = normalizedQuery,
                display = limit,
                start = start
            )

            val items = response.items.mapNotNull(BookSearchItemResult::from)

            val nextStart = start + limit
            val hasNext = response.items.size == limit &&
                    nextStart <= NAVER_START_MAX &&
                    nextStart <= response.total

            CursorPageResult(
                nextCursor = if (hasNext) nextStart.toString() else null,
                hasNext = hasNext,
                items = items
            )
        } catch (e: ServiceException) {
            throw e
        } catch (e: Exception) {
            throw ServiceException(
                errorCode = ErrorCode.EXTERNAL_API_COMMUNICATION_FAILED,
                message = "네이버 책 검색 API 호출에 실패했어요. query=$normalizedQuery, start=$start, limit=$limit"
            )
        }
    }

    /**
     * 파라미터의 인자를 검증합니다.
     *
     * @param query 검색어
     * @param limit 검색 개수
     * @param sort 정렬 유형
     */
    private fun validate(query: String, limit: Int) {
        if (query.isBlank()) {
            throw ServiceException(
                errorCode = ErrorCode.INVALID_INPUT_VALUE,
                message = "파라미터 query의 인자가 없어요. 파라미터 query는 필수 입력 항목이에요."
            )
        }

        if (limit !in 1..NAVER_DISPLAY_MAX) {
            throw ServiceException(
                errorCode = ErrorCode.INVALID_INPUT_VALUE,
                message = "파라미터 limit의 범위를 확인해 주세요. limit는 ${NAVER_START_MIN} 이상 ${NAVER_START_MAX} 이하의 값이어야 해요."
            )
        }
    }
}
