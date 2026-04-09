package kr.ac.kumoh.polaris.library.implement

import kr.ac.kumoh.polaris.library.entity.LibraryOperatingHour
import kr.ac.kumoh.polaris.library.repository.LibraryOperatingHourRepository
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

    fun findTodayOperatingHourMap(
        libraryIds: Collection<Long>,
        date: LocalDate
    ): Map<Long, LibraryOperatingHour> {
        if (libraryIds.isEmpty()) {
            return emptyMap()
        }

        return libraryOperatingHourRepository.findAllByLibraryIdsAndWeekday(
            libraryIds = libraryIds.distinct(),
            weekday = date.dayOfWeek.value
        ).associateBy { operatingHour ->
            requireNotNull(operatingHour.library.id) {
                "운영시간에 연결된 도서관 ID가 없습니다. operatingHourId=${operatingHour.id}"
            }
        }
    }
}
