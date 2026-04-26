package kr.ac.kumoh.polaris.bookvote.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.bookvote.presentation.request.BookVoteRequest
import kr.ac.kumoh.polaris.bookvote.service.BookVoteService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "도서 투표")
@RestController
@RequestMapping("/api/v1/books")
class BookVoteController(
    private val bookVoteService: BookVoteService
) {
    @Operation(
        summary = "도서 추천/비추천 투표",
        description = """
            <p>인증된 사용자가 특정 도서에 추천 또는 비추천 투표를 합니다.</p>
            <p>같은 도서에 다시 투표하면 새 투표를 만들지 않고 기존 투표 값을 수정합니다.</p>
            <p>투표 값은 <code>RECOMMEND</code>, <code>NOT_RECOMMEND</code>만 허용됩니다.</p>
        """,
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
        ]
    )
    @PutMapping("/{isbn}/vote")
    fun voteBook(
        @PathVariable isbn: String,
        @Valid @RequestBody request: BookVoteRequest,
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<Void> {
        bookVoteService.voteBook(authenticatedUser.id, isbn, request.voteType)
        return ResponseEntity.noContent().build()
    }
}
