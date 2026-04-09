package kr.ac.kumoh.polaris.library.entity

enum class ClosedRuleType {
    WEEKLY,               // 매주 특정 요일
    MONTHLY_NTH_WEEKDAY,  // 매월 n번째 특정 요일
    MONTHLY_DAY,          // 매월 특정 일자
    HOLIDAY               // 공휴일 휴관
}
