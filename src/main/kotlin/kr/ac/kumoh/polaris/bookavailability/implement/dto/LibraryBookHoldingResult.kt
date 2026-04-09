package kr.ac.kumoh.polaris.bookavailability.implement.dto

data class LibraryBookHoldingResult(
    val libraryId: Long,
    val isbn: String,
    val hasBook: Boolean?,
    val loanAvailable: Boolean?,
    val availabilityStatus: LibraryBookAvailabilityStatus,
    val openNow: Boolean
)
