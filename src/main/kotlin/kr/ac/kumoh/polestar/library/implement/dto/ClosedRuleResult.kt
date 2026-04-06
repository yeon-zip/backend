package kr.ac.kumoh.polestar.library.implement.dto

data class ClosedRuleResult(
    val ruleType: String,
    val weekday: Int?,
    val nthWeek: Int?,
    val monthDay: Int?
)
