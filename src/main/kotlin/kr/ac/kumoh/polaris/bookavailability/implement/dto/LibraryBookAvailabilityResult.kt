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
    ON_LOAN,
    UNAVAILABLE,
    UNKNOWN;

    companion object {
        fun resolve(
            hasBook: Boolean?,
            loanAvailable: Boolean?
        ): LibraryBookAvailabilityStatus =
            when {
                hasBook == true && loanAvailable == true -> AVAILABLE
                hasBook == true && loanAvailable == false -> ON_LOAN
                hasBook == false -> UNAVAILABLE
                else -> UNKNOWN
            }
    }
}
