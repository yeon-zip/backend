package kr.ac.kumoh.polestar.library.presentation.response

import kr.ac.kumoh.polestar.library.implement.dto.NearbyLibraryItemResult

data class NearbyLibraryItemResponse(
    val libraryId: Long,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val homepageUrl: String?,
    val tel: String?,
    val distanceKm: Double,
    val openNow: Boolean
) {
    companion object {
        fun from(result: NearbyLibraryItemResult): NearbyLibraryItemResponse =
            NearbyLibraryItemResponse(
                libraryId = result.libraryId,
                name = result.name,
                address = result.address,
                latitude = result.latitude,
                longitude = result.longitude,
                homepageUrl = result.homepageUrl,
                tel = result.tel,
                distanceKm = result.distanceKm,
                openNow = result.openNow
            )
    }
}
