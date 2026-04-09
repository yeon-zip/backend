package kr.ac.kumoh.polaris.bookavailability.implement.dto

class LibraryBookAvailabilityResult(
    val hasBook: Boolean?,
    val loanAvailable: Boolean?,
    val status: LibraryBookAvailabilityStatus
) {
    companion object {
        fun unknown(): LibraryBookAvailabilityResult =
            LibraryBookAvailabilityResult(
                hasBook = null,
                loanAvailable = null,
                status = LibraryBookAvailabilityStatus.UNKNOWN
            )
    }
}

enum class LibraryBookAvailabilityStatus {
    AVAILABLE,
    UNAVAILABLE,
    UNKNOWN
}
