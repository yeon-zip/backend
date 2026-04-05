package kr.ac.kumoh.polestar.library.implement.dto

data class LibraryDetailResult(
    val libraryId: Long,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val homepageUrl: String?,
    val tel: String?,
    val openNow: Boolean,
    val todayOperatingHour: TodayOperatingHourResult?,
    val weeklyOperatingHours: List<WeeklyOperatingHourResult>,
    val closedRules: List<ClosedRuleResult>
)
