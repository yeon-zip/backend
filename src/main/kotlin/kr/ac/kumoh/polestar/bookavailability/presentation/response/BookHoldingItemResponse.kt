package kr.ac.kumoh.polestar.bookavailability.presentation.response

import kr.ac.kumoh.polestar.bookavailability.implement.dto.BookHoldingItemResult
import kr.ac.kumoh.polestar.bookavailability.implement.dto.LibraryBookAvailabilityStatus

data class BookHoldingItemResponse(
    val libraryId: Long,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val distanceKm: Double,
    val hasBook: Boolean?,
    val loanAvailable: Boolean?,
    val availabilityStatus: LibraryBookAvailabilityStatus,
    val openNow: Boolean
) {
    companion object {
        fun from(result: BookHoldingItemResult): BookHoldingItemResponse =
            BookHoldingItemResponse(
                libraryId = result.libraryId,
                name = result.name,
                address = result.address,
                latitude = result.latitude,
                longitude = result.longitude,
                distanceKm = result.distanceKm,
                hasBook = result.hasBook,
                loanAvailable = result.loanAvailable,
                availabilityStatus = result.availabilityStatus,
                openNow = result.openNow
            )
    }
}
