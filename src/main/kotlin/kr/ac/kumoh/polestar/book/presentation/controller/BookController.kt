package kr.ac.kumoh.polestar.book.presentation.controller

import kr.ac.kumoh.polestar.book.presentation.response.BookResponse
import kr.ac.kumoh.polestar.book.presentation.response.BookSearchItemResponse
import kr.ac.kumoh.polestar.book.service.BookSearchService
import kr.ac.kumoh.polestar.book.service.BookService
import kr.ac.kumoh.polestar.bookavailability.presentation.response.BookHoldingItemResponse
import kr.ac.kumoh.polestar.bookavailability.service.BookAvailabilityService
import kr.ac.kumoh.polestar.global.dto.CursorPageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/books")
class BookController(
    private val bookService: BookService,
    private val bookAvailabilityService: BookAvailabilityService,
    private val bookSearchService: BookSearchService
) {
    @GetMapping("/search")
    fun searchBooks(
        @RequestParam query: String,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "sim") sort: String
    ): ResponseEntity<CursorPageResponse<BookSearchItemResponse>> {
        val result = bookSearchService.searchBooks(
            query = query,
            cursor = cursor,
            limit = limit,
            sort = sort
        )

        return ResponseEntity.ok(
            CursorPageResponse.from(result, BookSearchItemResponse::from)
        )
    }

    @GetMapping("/{isbn}")
    fun getBook(
        @PathVariable isbn: String
    ): ResponseEntity<BookResponse> {
        val result = bookService.getBook(isbn)

        return ResponseEntity.ok(BookResponse.from(result))
    }

    @GetMapping("/{isbn}/libraries")
    fun getBookLibraries(
        @PathVariable isbn: String,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(defaultValue = "5.0") radiusKm: Double,
        @RequestParam(required = false) loanAvailable: Boolean?,
        @RequestParam(required = false) openNow: Boolean?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<CursorPageResponse<BookHoldingItemResponse>> {
        val result = bookAvailabilityService.getLibrariesByBook(
            isbn = isbn,
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            loanAvailable = loanAvailable,
            openNow = openNow,
            cursor = cursor,
            limit = limit
        )

        return ResponseEntity.ok(
            CursorPageResponse.from(result, BookHoldingItemResponse::from)
        )
    }
}
