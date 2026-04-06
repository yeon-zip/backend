package kr.ac.kumoh.polestar.library.implement

import kr.ac.kumoh.polestar.library.repository.PublicHolidayRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PublicHolidayReader(
    private val publicHolidayRepository: PublicHolidayRepository
) {
    fun isHoliday(date: LocalDate): Boolean =
        publicHolidayRepository.existsByHolidayDate(date)
}
