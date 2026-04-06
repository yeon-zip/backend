package kr.ac.kumoh.polestar.library.presentation.response

import kr.ac.kumoh.polestar.library.implement.dto.WeeklyOperatingHourResult
import java.time.LocalTime

data class WeeklyOperatingHourResponse(
    val weekday: Int,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
    val isClosed: Boolean
) {
    companion object {
        fun from(result: WeeklyOperatingHourResult): WeeklyOperatingHourResponse =
            WeeklyOperatingHourResponse(
                weekday = result.weekday,
                openTime = result.openTime,
                closeTime = result.closeTime,
                isClosed = result.isClosed
            )
    }
}
