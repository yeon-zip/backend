package kr.ac.kumoh.polaris.library.implement

import kr.ac.kumoh.polaris.library.entity.ClosedRuleType
import kr.ac.kumoh.polaris.library.entity.LibraryClosedRule
import kr.ac.kumoh.polaris.library.entity.LibraryOperatingHour
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale

@Component
class LibraryAvailabilityChecker {
    fun isOpen(
        dateTime: LocalDateTime,
        operatingHour: LibraryOperatingHour,
        closedRules: List<LibraryClosedRule>,
        isHoliday: Boolean
    ): Boolean {
        val date = dateTime.toLocalDate()

        if (isClosedByHoliday(closedRules, isHoliday)) return false
        if (isClosedByRule(date, closedRules)) return false

        return operatingHour.isOpenAt(dateTime.toLocalTime())
    }

    private fun isClosedByHoliday(
        rules: List<LibraryClosedRule>,
        isHoliday: Boolean
    ): Boolean =
        isHoliday && rules.any { it.ruleType == ClosedRuleType.HOLIDAY }

    private fun isClosedByRule(
        date: LocalDate,
        rules: List<LibraryClosedRule>
    ): Boolean {
        val weekday = date.dayOfWeek.value
        val nthWeek = date.weekOfMonth()
        val monthDay = date.dayOfMonth

        return rules.any { rule ->
            when (rule.ruleType) {
                ClosedRuleType.WEEKLY -> rule.weekday == weekday
                ClosedRuleType.MONTHLY_NTH_WEEKDAY -> rule.weekday == weekday && rule.nthWeek == nthWeek
                ClosedRuleType.MONTHLY_DAY -> rule.monthDay == monthDay
                ClosedRuleType.HOLIDAY -> false
            }
        }
    }

    private fun LocalDate.weekOfMonth(): Int =
        get(WeekFields.of(Locale.KOREA).weekOfMonth())
}
