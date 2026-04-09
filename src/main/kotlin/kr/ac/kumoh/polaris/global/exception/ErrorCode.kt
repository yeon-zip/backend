package kr.ac.kumoh.polaris.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val message: String
) {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 값을 입력했어요. 올바른 값을 입력해 주세요!"),
    INVALID_ISBN(HttpStatus.BAD_REQUEST, "isbn은 13자리 숫자여야 합니다."),
    INVALID_QUERY(HttpStatus.BAD_REQUEST, "query는 비어 있을 수 없습니다."),
    INVALID_SORT(HttpStatus.BAD_REQUEST, "sort는 sim 또는 date 여야 합니다."),
    INVALID_RADIUS_KM(HttpStatus.BAD_REQUEST, "radiusKm은 0보다 커야 합니다."),
    INVALID_LIMIT(HttpStatus.BAD_REQUEST, "limit은 1 이상 100 이하여야 합니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "cursor 형식이 올바르지 않습니다."),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "도서를 찾을 수 없습니다."),
    LIBRARY_NOT_FOUND(HttpStatus.NOT_FOUND, "도서관을 찾을 수 없습니다."),
    NAVER_SEARCH_FAILED(HttpStatus.BAD_GATEWAY, "네이버 책 검색 호출에 실패했습니다.")
}
