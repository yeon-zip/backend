package kr.ac.kumoh.polaris.library.implement

import kr.ac.kumoh.polaris.library.entity.LibraryClosedRule
import kr.ac.kumoh.polaris.library.entity.LibraryOperatingHour
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class LibraryOpenStatusResolver(
    private val libraryOperatingHourReader: LibraryOperatingHourReader,
    private val libraryClosedRuleReader: LibraryClosedRuleReader,
    private val publicHolidayReader: PublicHolidayReader,
    private val libraryAvailabilityChecker: LibraryAvailabilityChecker
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun isOpenNow(
        libraryId: Long,
        now: LocalDateTime
    ): Boolean {
        val operatingHour = libraryOperatingHourReader.findTodayOperatingHour(
            libraryId = libraryId,
            date = now.toLocalDate()
        ) ?: return false

        return isOpenNow(
            now = now,
            operatingHour = operatingHour,
            closedRules = libraryClosedRuleReader.findClosedRules(libraryId),
        )
    }

    fun isOpenNow(
        now: LocalDateTime,
        operatingHour: LibraryOperatingHour?,
        closedRules: List<LibraryClosedRule>
    ): Boolean {
        if (operatingHour == null) {
            return false
        }

        return libraryAvailabilityChecker.isOpen(
            dateTime = now,
            operatingHour = operatingHour,
            closedRules = closedRules,
            isHoliday = publicHolidayReader.isHoliday(now.toLocalDate())
        )
    }

    fun getOpenNowStatuses(
        libraryIds: Collection<Long>,
        now: LocalDateTime
    ): List<LibraryOpenNowStatus> {
        if (libraryIds.isEmpty()) {
            return emptyList()
        }

        val startedAt = System.nanoTime()
        val distinctLibraryIds = libraryIds.distinct()
        val date = now.toLocalDate()
        val operatingHourByLibraryId = libraryOperatingHourReader.findTodayOperatingHourMap(
            libraryIds = distinctLibraryIds,
            date = date
        )
        val closedRuleMap = libraryClosedRuleReader.findClosedRuleMap(distinctLibraryIds)
        val isHoliday = publicHolidayReader.isHoliday(date)

        val openNowStatuses = distinctLibraryIds.map { libraryId ->
            val operatingHour = operatingHourByLibraryId[libraryId]
            val openNow = if (operatingHour == null) {
                false
            } else {
                libraryAvailabilityChecker.isOpen(
                    dateTime = now,
                    operatingHour = operatingHour,
                    closedRules = closedRuleMap[libraryId].orEmpty(),
                    isHoliday = isHoliday
                )
            }

            LibraryOpenNowStatus(
                libraryId = libraryId,
                openNow = openNow
            )
        }

        log.info(
            "도서관 운영시간 배치 조회 완료 - libraryCount={}, weekday={}, elapsedMs={}",
            distinctLibraryIds.size,
            now.dayOfWeek.value,
            elapsedMillis(startedAt)
        )

        return openNowStatuses
    }

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt) / 1_000_000
}

data class LibraryOpenNowStatus(
    val libraryId: Long,
    val openNow: Boolean
)
