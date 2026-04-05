package kr.ac.kumoh.polestar.bookavailability.presentation.controller

import kr.ac.kumoh.polestar.bookavailability.presentation.response.BookHoldingItemResponse
import kr.ac.kumoh.polestar.bookavailability.service.BookAvailabilityService
import kr.ac.kumoh.polestar.global.dto.CursorPageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/book-availability")
class BookHoldingController(
    private val bookAvailabilityService: BookAvailabilityService
) {
    @GetMapping
    fun getBookHoldings(
        @RequestParam isbn: String,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(defaultValue = "5.0") radiusKm: Double,
        @RequestParam(required = false) loanAvailable: Boolean?,
        @RequestParam(required = false) openNow: Boolean?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<CursorPageResponse<BookHoldingItemResponse>> {
        val result = bookAvailabilityService.getBookHoldings(
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
