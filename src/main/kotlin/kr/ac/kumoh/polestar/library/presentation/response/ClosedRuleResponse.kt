package kr.ac.kumoh.polestar.library.presentation.response

import kr.ac.kumoh.polestar.library.implement.dto.ClosedRuleResult

data class ClosedRuleResponse(
    val ruleType: String,
    val weekday: Int?,
    val nthWeek: Int?,
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
