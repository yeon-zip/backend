package kr.ac.kumoh.polaris.library.implement.dto

data class ClosedRuleResult(
    val ruleType: String,
    val weekday: Int?,
    val nthWeek: Int?,
    val monthDay: Int?
)
