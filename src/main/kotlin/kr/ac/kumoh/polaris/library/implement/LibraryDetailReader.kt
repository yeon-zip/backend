package kr.ac.kumoh.polaris.library.implement

import kr.ac.kumoh.polaris.library.entity.LibraryClosedRule
import kr.ac.kumoh.polaris.library.entity.LibraryOperatingHour
import kr.ac.kumoh.polaris.library.implement.dto.ClosedRuleResult
import kr.ac.kumoh.polaris.library.implement.dto.LibraryDetailResult
import kr.ac.kumoh.polaris.library.implement.dto.TodayOperatingHourResult
import kr.ac.kumoh.polaris.library.implement.dto.WeeklyOperatingHourResult
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class LibraryDetailReader(
    private val libraryReader: LibraryReader,
    private val libraryOperatingHourReader: LibraryOperatingHourReader,
    private val libraryClosedRuleReader: LibraryClosedRuleReader,
    private val libraryOpenStatusResolver: LibraryOpenStatusResolver
) {
    fun read(libraryId: Long): LibraryDetailResult {
        val library = libraryReader.findByIdOrThrow(libraryId)
        val resolvedLibraryId = requireNotNull(library.id) {
            "저장된 도서관 엔티티에 ID가 없습니다. libCode=${library.libCode}"
        }

        val now = LocalDateTime.now()
        val todayOperatingHour = libraryOperatingHourReader.findTodayOperatingHour(
            libraryId = resolvedLibraryId,
            date = now.toLocalDate()
        )
        val weeklyOperatingHours = libraryOperatingHourReader.findWeeklyOperatingHours(resolvedLibraryId)
            .sortedBy { it.weekday }
        val closedRules = libraryClosedRuleReader.findClosedRules(resolvedLibraryId)

        return LibraryDetailResult(
            libraryId = resolvedLibraryId,
            name = library.name,
            address = library.address.toString(),
            latitude = library.location.latitude,
            longitude = library.location.longitude,
            homepageUrl = library.contactInfo.homepageUrl,
            tel = library.contactInfo.tel,
            openNow = libraryOpenStatusResolver.isOpenNow(
                now = now,
                operatingHour = todayOperatingHour,
                closedRules = closedRules
            ),
            isBookmarked = false,
            todayOperatingHour = todayOperatingHour?.toTodayResult(),
            weeklyOperatingHours = weeklyOperatingHours.map { it.toWeeklyResult() },
            closedRules = closedRules.map { it.toClosedRuleResult() }
        )
    }

    private fun LibraryOperatingHour.toTodayResult(): TodayOperatingHourResult =
        TodayOperatingHourResult(
            weekday = weekday,
            openTime = openTime,
            closeTime = closeTime,
            isClosed = isClosed
        )

    private fun LibraryOperatingHour.toWeeklyResult(): WeeklyOperatingHourResult =
        WeeklyOperatingHourResult(
            weekday = weekday,
            openTime = openTime,
            closeTime = closeTime,
            isClosed = isClosed
        )

    private fun LibraryClosedRule.toClosedRuleResult(): ClosedRuleResult =
        ClosedRuleResult(
            ruleType = ruleType.name,
            weekday = weekday,
            nthWeek = nthWeek,
            monthDay = monthDay
        )
}
