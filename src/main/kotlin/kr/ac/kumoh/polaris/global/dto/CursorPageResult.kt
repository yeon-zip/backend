package kr.ac.kumoh.polaris.global.dto

/**
 * 커서 기반 페이지네이션 내부 DTO
 *
 * 커서 기반 페이지네이션 결과를 담는 내부 DTO로, Service, Implement에서 사용해야 합니다.
 *
 * @param T 페이지 항목 타입
 * @property hasNext 다음 페이지 존재 여부
 * @property nextCursor 다음 페이지를 요청할 때 사용할 커서 (`null`이면 마지막 페이지를 뜻함)
 * @property items 현재 페이지의 데이터 목록
 */
data class CursorPageResult<T>(
    val hasNext: Boolean,
    val nextCursor: String?,
    val items: List<T>
)
