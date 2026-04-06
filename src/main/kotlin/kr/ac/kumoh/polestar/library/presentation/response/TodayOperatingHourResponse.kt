package kr.ac.kumoh.polestar.library.presentation.response

import kr.ac.kumoh.polestar.library.implement.dto.TodayOperatingHourResult
import java.time.LocalTime

data class TodayOperatingHourResponse(
    val weekday: Int,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
    val isClosed: Boolean
) {
    companion object {
        fun from(result: TodayOperatingHourResult): TodayOperatingHourResponse =
            TodayOperatingHourResponse(
                weekday = result.weekday,
                openTime = result.openTime,
                closeTime = result.closeTime,
                isClosed = result.isClosed
            )
    }
}
