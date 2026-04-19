package kr.ac.kumoh.polaris.user.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.user.presentation.response.CurrentUserResponse
import kr.ac.kumoh.polaris.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "사용자")
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {
    @Operation(
        summary = "현재 사용자 조회",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<CurrentUserResponse> {
        val result = userService.getCurrentUser(authenticatedUser.id)
        return ResponseEntity.ok(CurrentUserResponse.from(result))
    }
}
