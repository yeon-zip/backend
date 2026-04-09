package kr.ac.kumoh.polaris.library.implement.dto

data class NearbyLibraryItemResult(
    val libraryId: Long,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val homepageUrl: String?,
    val tel: String?,
    val distanceKm: Double,
    val openNow: Boolean
)
