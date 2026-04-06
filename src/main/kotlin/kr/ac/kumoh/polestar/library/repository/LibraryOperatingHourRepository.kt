package kr.ac.kumoh.polestar.library.repository

import kr.ac.kumoh.polestar.library.entity.LibraryOperatingHour
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LibraryOperatingHourRepository : JpaRepository<LibraryOperatingHour, Long> {
    fun findByLibraryIdAndWeekday(libraryId: Long, weekday: Int): LibraryOperatingHour?
    fun findAllByLibraryId(libraryId: Long): List<LibraryOperatingHour>

    @Query(
        """
        select operatingHour
        from LibraryOperatingHour operatingHour
        join fetch operatingHour.library library
        where library.id in :libraryIds
          and operatingHour.weekday = :weekday
        """
    )
    fun findAllByLibraryIdsAndWeekday(
        @Param("libraryIds") libraryIds: Collection<Long>,
        @Param("weekday") weekday: Int
    ): List<LibraryOperatingHour>
}
