package kr.ac.kumoh.polaris.library.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.ac.kumoh.polaris.library.implement.dto.LibraryDetailResult

data class LibraryResponse(
    @Schema(description = "도서관 ID입니다. 최대 길이는 19자입니다.", example = "3372036854775808", nullable = false)
    val libraryId: String,
    @Schema(description = "도서관명입니다.", example = "구미시립양포도서관", nullable = false)
    val name: String,
    @Schema(description = "도서관 주소입니다.", example = "경상북도 구미시 옥계북로 51", nullable = false)
    val address: String,
    @Schema(description = "도서관이 위치한 곳의 위도입니다.", example = "36.1387724", nullable = true)
    val latitude: Double?,
    @Schema(description = "도서관이 위치한 곳의 경도입니다.", example = "128.4187321", nullable = true)
    val longitude: Double?,
    @Schema(description = "도서관 홈페이지 URL입니다.", example = "https://lib.gumi.go.kr", nullable = true)
    val homepageUrl: String?,
    @Schema(description = "도서관 전화번호입니다.", example = "054-480-4770", nullable = false)
    val tel: String?,
    @Schema(description = "도서관 운영 여부입니다.", example = "true", nullable = false)
    val openNow: Boolean,
    @Schema(description = "현재 인증 사용자의 찜 여부입니다.", example = "false", nullable = false)
    val isBookmarked: Boolean,
    val todayOperatingHour: TodayOperatingHourResponse?,
    val weeklyOperatingHours: List<WeeklyOperatingHourResponse>,
    val closedRules: List<ClosedRuleResponse>
) {
    companion object {
        fun from(result: LibraryDetailResult): LibraryResponse =
            LibraryResponse(
                libraryId = result.libraryId.toString(),
                name = result.name,
                address = result.address,
                latitude = result.latitude,
                longitude = result.longitude,
                homepageUrl = result.homepageUrl,
                tel = result.tel,
                openNow = result.openNow,
                isBookmarked = result.isBookmarked,
                todayOperatingHour = result.todayOperatingHour?.let(TodayOperatingHourResponse::from),
                weeklyOperatingHours = result.weeklyOperatingHours.map(WeeklyOperatingHourResponse::from),
                closedRules = result.closedRules.map(ClosedRuleResponse::from)
            )
    }
}
