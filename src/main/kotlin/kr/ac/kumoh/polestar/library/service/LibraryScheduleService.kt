package kr.ac.kumoh.polestar.library.service

import kr.ac.kumoh.polestar.library.implement.LibraryAvailabilityChecker
import kr.ac.kumoh.polestar.library.implement.LibraryOperatingHourReader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class LibraryScheduleService(
    private val libraryOperatingHourReader: LibraryOperatingHourReader,
    private val libraryOpenChecker: LibraryAvailabilityChecker
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun isOpenNow(
        libraryId: Long,
        now: LocalDateTime
    ): Boolean {
        // 도서관 운영 시간을 조회합니다.
        val operatingHour = libraryOperatingHourReader.findTodayOperatingHour(libraryId, now.toLocalDate())
            ?: return false

        return libraryOpenChecker.isOpen(
            libraryId = libraryId,
            dateTime = now,
            operatingHour = operatingHour
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
        val operatingHourRecords = libraryOperatingHourReader.findTodayOperatingHours(
            libraryIds = distinctLibraryIds,
            date = now.toLocalDate()
        )
        val operatingHourByLibraryId = operatingHourRecords.associateBy(
            keySelector = { it.libraryId },
            valueTransform = { it.operatingHour }
        )

        val openNowStatuses = distinctLibraryIds.map { libraryId ->
            val operatingHour = operatingHourByLibraryId[libraryId]
            val openNow = if (operatingHour == null) {
                false
            } else {
                libraryOpenChecker.isOpen(
                    libraryId = libraryId,
                    dateTime = now,
                    operatingHour = operatingHour
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

    fun getTodayOperatingHour(libraryId: Long, date: LocalDate) =
        libraryOperatingHourReader.findTodayOperatingHour(libraryId, date)

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt) / 1_000_000
}

data class LibraryOpenNowStatus(
    val libraryId: Long,
    val openNow: Boolean
)
