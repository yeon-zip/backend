package kr.ac.kumoh.polestar.library.implement

import kr.ac.kumoh.polestar.library.entity.ClosedRuleType
import kr.ac.kumoh.polestar.library.entity.LibraryClosedRule
import kr.ac.kumoh.polestar.library.entity.LibraryOperatingHour
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale

@Component
class LibraryAvailabilityChecker(
    private val libraryClosedRuleReader: LibraryClosedRuleReader,
    private val publicHolidayReader: PublicHolidayReader
) {
    fun isOpen(
        libraryId: Long,
        dateTime: LocalDateTime,
        operatingHour: LibraryOperatingHour
    ): Boolean {
        val date = dateTime.toLocalDate()
        val rules = libraryClosedRuleReader.findClosedRules(libraryId)

        if (isClosedByHoliday(date, rules)) return false
        if (isClosedByRule(date, rules)) return false

        return operatingHour.isOpenAt(dateTime.toLocalTime())
    }

    private fun isClosedByHoliday(
        date: LocalDate,
        rules: List<LibraryClosedRule>
    ): Boolean =
        publicHolidayReader.isHoliday(date) && rules.any { it.ruleType == ClosedRuleType.HOLIDAY }

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
