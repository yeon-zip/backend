package kr.ac.kumoh.polaris.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val message: String
) {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 값을 입력했어요. 입력 값을 다시 확인해 주세요."),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 도서를 찾을 수 없어요. 입력 값을 다시 확인해 주세요."),
    LIBRARY_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 도서관을 찾을 수 없어요. 입력 값을 다시 확인해 주세요."),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 찜한 항목이에요."),
    EXTERNAL_API_COMMUNICATION_FAILED(HttpStatus.BAD_GATEWAY, "외부 API와의 통신에 실패했어요. 잠시 후 다시 시도하세요."),
    EXTERNAL_API_RESPONSE_PARSE_FAILED(HttpStatus.BAD_GATEWAY, "외부 API 응답을 처리하는 데 실패했어요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 사용자를 찾을 수 없어요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰이에요. 다시 로그인해 주세요."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "refresh token을 찾을 수 없어요. 다시 로그인해 주세요."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "refresh token이 만료되었어요. 다시 로그인해 주세요."),
    OIDC_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "Kakao OIDC 로그인에 실패했어요."),
    OIDC_INVALID_CHANNEL(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 채널이에요."),
    OIDC_TARGET_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않은 앱 로그인 대상이에요."),
    OIDC_PROOF_REQUIRED(HttpStatus.BAD_REQUEST, "앱 교환 코드에는 proof가 필요해요."),
    OIDC_UNSUPPORTED_CODE_CHALLENGE_METHOD(HttpStatus.BAD_REQUEST, "지원하지 않는 code challenge method예요."),
    OIDC_INVALID_CODE_CHALLENGE(HttpStatus.BAD_REQUEST, "유효하지 않은 code challenge예요."),
    OIDC_INVALID_CODE_VERIFIER(HttpStatus.BAD_REQUEST, "유효하지 않은 code verifier예요."),
    OIDC_PROOF_MISMATCH(HttpStatus.UNAUTHORIZED, "앱 proof 검증에 실패했어요."),
    OIDC_EXCHANGE_CODE_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 교환 코드예요."),
    OIDC_EXCHANGE_CODE_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 교환 코드예요."),
    OIDC_EXCHANGE_CODE_ALREADY_CONSUMED(HttpStatus.CONFLICT, "이미 사용된 교환 코드예요."),
    OIDC_EXCHANGE_TARGET_MISMATCH(HttpStatus.UNAUTHORIZED, "교환 코드의 targetId가 일치하지 않아요."),
    OIDC_TERMINAL_ISSUANCE_FAILURE(HttpStatus.SERVICE_UNAVAILABLE, "교환 코드 발급을 완료할 수 없어요. 다시 로그인해 주세요."),
    OIDC_NON_APP_CALLBACK_DISABLED(HttpStatus.FORBIDDEN, "비앱 OIDC 콜백은 비활성화되어 있어요.")
}
