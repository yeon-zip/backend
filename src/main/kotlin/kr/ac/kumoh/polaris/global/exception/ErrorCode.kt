package kr.ac.kumoh.polaris.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val message: String
) {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 값을 입력했어요. 입력 값을 다시 확인해 주세요."),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 도서를 찾을 수 없어요. 입력 값을 다시 확인해 주세요."),
    LIBRARY_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 도서관을 찾을 수 없어요. 입력 값을 다시 확인해 주세요."),
    EXTERNAL_API_COMMUNICATION_FAILED(HttpStatus.BAD_GATEWAY, "외부 API와의 통신에 실패했어요. 잠시 후 다시 시도하세요."),
    EXTERNAL_API_RESPONSE_PARSE_FAILED(HttpStatus.BAD_GATEWAY, "외부 API 응답을 처리하는 데 실패했어요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 사용자를 찾을 수 없어요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰이에요. 다시 로그인해 주세요."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "refresh token을 찾을 수 없어요. 다시 로그인해 주세요."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "refresh token이 만료되었어요. 다시 로그인해 주세요.")

}
