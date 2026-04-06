package kr.ac.kumoh.polestar.library.presentation.response

import kr.ac.kumoh.polestar.library.implement.dto.LibraryDetailResult

data class LibraryResponse(
    val libraryId: Long,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val homepageUrl: String?,
    val tel: String?,
    val openNow: Boolean,
    val todayOperatingHour: TodayOperatingHourResponse?,
    val weeklyOperatingHours: List<WeeklyOperatingHourResponse>,
    val closedRules: List<ClosedRuleResponse>
) {
    companion object {
        fun from(result: LibraryDetailResult): LibraryResponse =
            LibraryResponse(
                libraryId = result.libraryId,
                name = result.name,
                address = result.address,
                latitude = result.latitude,
                longitude = result.longitude,
                homepageUrl = result.homepageUrl,
                tel = result.tel,
                openNow = result.openNow,
                todayOperatingHour = result.todayOperatingHour?.let(TodayOperatingHourResponse::from),
                weeklyOperatingHours = result.weeklyOperatingHours.map(WeeklyOperatingHourResponse::from),
                closedRules = result.closedRules.map(ClosedRuleResponse::from)
            )
    }
}
