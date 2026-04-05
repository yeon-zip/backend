package kr.ac.kumoh.polestar.library.repository

import kr.ac.kumoh.polestar.library.entity.PublicHoliday
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface PublicHolidayRepository : JpaRepository<PublicHoliday, Long> {
    fun existsByHolidayDate(holidayDate: LocalDate): Boolean
}
