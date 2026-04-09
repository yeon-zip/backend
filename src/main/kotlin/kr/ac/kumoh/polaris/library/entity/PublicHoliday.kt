package kr.ac.kumoh.polaris.library.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "public_holiday",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_public_holiday_date", columnNames = ["holiday_date"])
    ]
)
class PublicHoliday(
    holidayDate: LocalDate,
    holidayName: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "holiday_date", nullable = false)
    var holidayDate: LocalDate = holidayDate
        protected set

    @Column(name = "holiday_name", nullable = false, length = 100)
    var holidayName: String = holidayName
        protected set
}
