package kr.ac.kumoh.polestar.library.implement.dto

import java.time.LocalTime

data class TodayOperatingHourResult(
    val weekday: Int,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
    val isClosed: Boolean
)
