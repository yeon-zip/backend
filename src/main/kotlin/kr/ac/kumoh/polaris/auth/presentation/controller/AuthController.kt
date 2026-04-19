package kr.ac.kumoh.polaris.auth.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import kr.ac.kumoh.polaris.auth.presentation.response.AuthTokenResponse
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.auth.service.AuthTokenService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authTokenService: AuthTokenService
) {
    @Operation(summary = "Kakao OIDC 로그인 진입점")
    @GetMapping("/kakao/login")
    fun redirectToKakaoLogin(response: HttpServletResponse) {
        response.sendRedirect("/oauth2/authorization/kakao")
    }

    @Operation(summary = "refresh token으로 토큰 재발급")
    @PostMapping("/refresh")
    fun refresh(
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorizationHeader: String
    ): ResponseEntity<AuthTokenResponse> = ResponseEntity.ok(authTokenService.refresh(authorizationHeader))

    @Operation(
        summary = "로그아웃 또는 토큰 무효화",
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
