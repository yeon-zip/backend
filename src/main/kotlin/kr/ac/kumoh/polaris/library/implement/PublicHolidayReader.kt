package kr.ac.kumoh.polaris.library.implement

import kr.ac.kumoh.polaris.library.repository.PublicHolidayRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PublicHolidayReader(
    private val publicHolidayRepository: PublicHolidayRepository
) {
    fun isHoliday(date: LocalDate): Boolean =
        publicHolidayRepository.existsByHolidayDate(date)
}
