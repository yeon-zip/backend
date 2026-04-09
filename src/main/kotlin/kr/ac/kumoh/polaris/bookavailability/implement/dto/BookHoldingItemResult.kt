package kr.ac.kumoh.polaris.bookavailability.implement.dto

data class BookHoldingItemResult(
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
)
