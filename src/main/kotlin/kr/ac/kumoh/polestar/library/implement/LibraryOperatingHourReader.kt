package kr.ac.kumoh.polestar.library.implement

import kr.ac.kumoh.polestar.library.entity.LibraryOperatingHour
import kr.ac.kumoh.polestar.library.repository.LibraryOperatingHourRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class LibraryOperatingHourReader(
    private val libraryOperatingHourRepository: LibraryOperatingHourRepository
) {
    fun findTodayOperatingHour(libraryId: Long, date: LocalDate): LibraryOperatingHour? =
        libraryOperatingHourRepository.findByLibraryIdAndWeekday(
            libraryId = libraryId,
            weekday = date.dayOfWeek.value
        )

    fun findWeeklyOperatingHours(libraryId: Long): List<LibraryOperatingHour> =
        libraryOperatingHourRepository.findAllByLibraryId(libraryId)

    fun findTodayOperatingHours(
        libraryIds: Collection<Long>,
        date: LocalDate
    ): List<LibraryTodayOperatingHourRecord> {
        if (libraryIds.isEmpty()) {
            return emptyList()
        }

        return libraryOperatingHourRepository.findAllByLibraryIdsAndWeekday(
            libraryIds = libraryIds.distinct(),
            weekday = date.dayOfWeek.value
        ).map { operatingHour ->
            LibraryTodayOperatingHourRecord(
                libraryId = requireNotNull(operatingHour.library.id),
                operatingHour = operatingHour
            )
        }
    }
}

data class LibraryTodayOperatingHourRecord(
    val libraryId: Long,
    val operatingHour: LibraryOperatingHour
)
