package kr.ac.kumoh.polaris.auth.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import kr.ac.kumoh.polaris.auth.presentation.request.ExchangeCodeRequest
import kr.ac.kumoh.polaris.auth.presentation.response.AuthTokenResponse
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.auth.service.AppLoginResolver
import kr.ac.kumoh.polaris.auth.service.AuthTokenService
import kr.ac.kumoh.polaris.auth.service.LoginExchangeCodeService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authTokenService: AuthTokenService,
    private val appLoginResolver: AppLoginResolver,
    private val loginExchangeCodeService: LoginExchangeCodeService
) {
    @Operation(summary = "Kakao 로그인", description = "Kakao 로그인 페이지로 이동합니다.")
    @GetMapping("/kakao/login")
    fun redirectToKakaoLogin(
        response: HttpServletResponse,
        @Parameter(description = "모바일 앱 환경에서는 `app`을, 웹 환경에서는 `web`을 사용하세요. 기본 값은 `web`입니다.")
        @RequestParam(required = false) channel: String?,
        @Parameter(description = "모바일 앱 환경일 때 앱으로 되돌아가기 위한 앱의 Scheme을 입력해야 합니다.")
        @RequestParam(required = false) target: String?,
        @Parameter(description = "[PKCE] 애플리케이션에서 생성한 임의의 문자열 `code_verifier`를 특정 함수로 변환한 값을 입력하세요.")
        @RequestParam(required = false) codeChallenge: String?,
        @Parameter(description = "[PKCE] `code_verifier` 변환에 사용되는 함수를 입력하세요. `S256`을 사용해야 합니다.")
        @RequestParam(required = false) codeChallengeMethod: String?,
        request: jakarta.servlet.http.HttpServletRequest
    ) {
        val redirectPath = appLoginResolver.resolveLoginRedirect(
            request = request,
            channelValue = channel,
            targetIdValue = target,
            codeChallengeValue = codeChallenge,
            codeChallengeMethodValue = codeChallengeMethod
        )
        response.sendRedirect(redirectPath)
    }

    @Operation(summary = "인증 토큰 발급", description = """
        <p>Kakao 로그인 후 발급된 코드로 로그인할 수 있는 액세스 토큰 및 리프레시 토큰을 발급합니다.</p>
        <p>액세스 토큰은 메모리에 저장 후 Authorization 헤더에 담아 전송해야 합니다.</p>
        <p>리프레시 토큰은 쿠키나 Keychain, Keystore에 저장하고 <code>/api/v1/auth/refresh</code>로 요청할 때만 전송하도록 해야 합니다.</p>
        <p>액세스 토큰은 10분(600초), 리프레시 토큰은 1년(31536000초) 간 유효합니다.</p>
    """)
    @PostMapping("/exchange")
    fun exchange(
        @Valid @RequestBody request: ExchangeCodeRequest
    ): ResponseEntity<AuthTokenResponse> = ResponseEntity.ok(
        loginExchangeCodeService.redeem(
            code = request.code,
            targetId = request.targetId,
            codeVerifier = request.codeVerifier
        )
    )

    @Operation(summary = "인증 토큰 재발급", description = "리프레시 토큰을 사용해 액세스 토큰을 재발급하고 기존 리프레시 토큰을 무효화합니다.")
    @PostMapping("/refresh")
    fun refresh(
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorizationHeader: String
    ): ResponseEntity<AuthTokenResponse> = ResponseEntity.ok(authTokenService.refresh(authorizationHeader))

    @Operation(
        summary = "로그아웃 또는 토큰 무효화",
        description = "로그아웃을 위해 리프레시 토큰을 무효화합니다. 액세스 토큰이 저장돼 있다면 잠시 동안 로그인 상태일 수 있으므로 삭제해야 합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/logout")
    fun logout(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @RequestHeader(name = "Refresh-Token", required = false) refreshTokenHeader: String?
    ): ResponseEntity<Void> {
        authTokenService.logout(authenticatedUser.id, refreshTokenHeader)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
