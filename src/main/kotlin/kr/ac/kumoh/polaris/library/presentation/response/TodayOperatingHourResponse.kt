package kr.ac.kumoh.polaris.library.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.ac.kumoh.polaris.library.implement.dto.TodayOperatingHourResult
import java.time.LocalTime

data class TodayOperatingHourResponse(
    @Schema(description = "요일 값입니다. <code>1</code>(일요일), <code>2</code>(월요일), ..., <code>7</code>(토요일)까지의 값입니다. 휴관 규칙 유형에 따라 <code>null</code> 값일 수 있습니다.", example = "1", nullable = false)
    val weekday: Int,
    @Schema(description = "도서관 운영 시작 시각입니다. 제공하지 않는 경우 <code>null</null> 값일 수 있습니다.", example = "09:00", nullable = true)
    val openTime: LocalTime?,
    @Schema(description = "도서관 운영 종료 시각입니다. 제공하지 않는 경우 <code>null</null> 값일 수 있습니다.", example = "18:00", nullable = true)
    val closeTime: LocalTime?,
    @Schema(description = "도서관 운영 종료 여부입니다.", example = "false", nullable = false)
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
