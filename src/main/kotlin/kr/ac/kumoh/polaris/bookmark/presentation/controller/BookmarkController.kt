package kr.ac.kumoh.polaris.bookmark.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.bookmark.presentation.response.BookmarkedBooksResponse
import kr.ac.kumoh.polaris.bookmark.presentation.response.BookmarkedLibrariesResponse
import kr.ac.kumoh.polaris.bookmark.service.BookmarkService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "찜")
@RestController
@RequestMapping("/api/v1")
class BookmarkController(
    private val bookmarkService: BookmarkService
) {
    @Operation(
        summary = "도서관 찜 등록",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
        ]
    )
    @PostMapping("/libraries/{libraryId}/bookmark")
    fun bookmarkLibrary(
        @PathVariable libraryId: Long,
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<Void> {
        bookmarkService.bookmarkLibrary(authenticatedUser.id, libraryId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "도서관 찜 해제",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
        ]
    )
    @DeleteMapping("/libraries/{libraryId}/bookmark")
    fun unbookmarkLibrary(
        @PathVariable libraryId: Long,
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<Void> {
        bookmarkService.unbookmarkLibrary(authenticatedUser.id, libraryId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "찜한 도서관 목록 조회",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공"),
        ]
    )
    @GetMapping("/users/me/bookmarked-libraries")
    fun getBookmarkedLibraries(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<BookmarkedLibrariesResponse> {
        val result = bookmarkService.getBookmarkedLibraries(authenticatedUser.id)
        return ResponseEntity.ok(BookmarkedLibrariesResponse.from(result))
    }

    @Operation(
        summary = "도서 찜 등록",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
        ]
    )
    @PostMapping("/books/{isbn}/bookmark")
    fun bookmarkBook(
        @PathVariable isbn: String,
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<Void> {
        bookmarkService.bookmarkBook(authenticatedUser.id, isbn)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "도서 찜 해제",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공"),
        ]
    )
    @DeleteMapping("/books/{isbn}/bookmark")
    fun unbookmarkBook(
        @PathVariable isbn: String,
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<Void> {
        bookmarkService.unbookmarkBook(authenticatedUser.id, isbn)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "찜한 도서 목록 조회",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공"),
        ]
    )
    @GetMapping("/users/me/bookmarked-books")
    fun getBookmarkedBooks(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<BookmarkedBooksResponse> {
        val result = bookmarkService.getBookmarkedBooks(authenticatedUser.id)
        return ResponseEntity.ok(BookmarkedBooksResponse.from(result))
    }
}
