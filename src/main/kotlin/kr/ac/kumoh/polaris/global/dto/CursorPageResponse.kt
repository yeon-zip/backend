package kr.ac.kumoh.polaris.global.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 커서 기반 페이지네이션 응답 DTO
 *
 * 커서 기반 페이지네이션 결과를 담는 공통 응답 DTO로, Controller에서 사용해야 합니다.
 *
 * @param T 페이지 항목 타입
 * @property hasNext 다음 페이지 존재 여부
 * @property nextCursor 다음 페이지를 요청할 때 사용할 커서 (`null`이면 마지막 페이지를 뜻함)
 * @property items 현재 페이지의 데이터 목록
 */
data class CursorPageResponse<T>(
    @field:Schema(description = "다음 페이지가 있으면 true입니다.", example = "true", nullable = false)
    val hasNext: Boolean,

    @field:Schema(
        description = "다음 페이지 조회에 사용할 커서입니다. 다음 페이지가 없으면 null입니다.",
        example = "98",
        nullable = true
    )
    val nextCursor: String?,

    @field:Schema(description = "현재 페이지의 항목 목록입니다.", nullable = false)
    val items: List<T>
) {
    companion object {
        /**
         * 빈 페이지 응답을 생성합니다.
         */
        fun <T> empty(): CursorPageResponse<T> {
            return CursorPageResponse(
                hasNext = false,
                nextCursor = null,
                items = emptyList()
            )
        }

        /**
         * 페이지 응답을 생성합니다.
         */
        fun <T, R> from(
            result: CursorPageResult<T>,
            itemMapper: (T) -> R
        ): CursorPageResponse<R> =
            CursorPageResponse(
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
                items = result.items.map(itemMapper)
            )
    }
}
