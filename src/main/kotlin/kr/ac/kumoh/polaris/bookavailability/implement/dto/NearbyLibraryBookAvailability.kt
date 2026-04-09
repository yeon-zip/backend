package kr.ac.kumoh.polaris.bookavailability.implement.dto

data class NearbyLibraryBookAvailability(
    val libraryId: Long,
    val libCode: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val homepageUrl: String?,
    val tel: String?,
    val distanceKm: Double,
    val hasBook: Boolean?,
    val loanAvailable: Boolean?,
    val status: LibraryBookAvailabilityStatus
)
