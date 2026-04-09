package kr.ac.kumoh.polaris.library.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.ac.kumoh.polaris.library.implement.dto.ClosedRuleResult

data class ClosedRuleResponse(
    @Schema(description = "휴관 규칙 유형입니다. 매주 특정 요일(<code>WEEKLY</code>), 매월 n번째 특정 요일(<code>MONTHLY_NTH_WEEKDAY</code>), 매월 특정 일자(<code>MONTHLY_DAY</code>), 공휴일(<code>HOLIDAY</code>) 중 하나입니다.", example = "WEEKLY")
    val ruleType: String,
    @Schema(description = "요일 값입니다. <code>1</code>(일요일), <code>2</code>(월요일), ..., <code>7</code>(토요일)까지의 값입니다. 휴관 규칙 유형에 따라 <code>null</code> 값일 수 있습니다.", example = "1", nullable = true)
    val weekday: Int?,
    @Schema(description = "몇 번째 주인지 나타내는 값입니다. 휴관 규칙 유형에 따라 <code>null</code> 값일 수 있습니다.", example = "2", nullable = true)
    val nthWeek: Int?,
    @Schema(description = "일 값입니다. 휴관 규칙 유형에 따라 <code>null</code>일 수 있습니다.", example = "15", nullable = true)
    val monthDay: Int?
) {
    companion object {
        fun from(result: ClosedRuleResult): ClosedRuleResponse =
            ClosedRuleResponse(
                ruleType = result.ruleType,
                weekday = result.weekday,
                nthWeek = result.nthWeek,
                monthDay = result.monthDay
            )
    }
}
