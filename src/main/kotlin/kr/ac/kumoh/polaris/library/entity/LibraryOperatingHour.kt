package kr.ac.kumoh.polaris.library.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "library_operating_hour")
class LibraryOperatingHour(
    library: Library,
    weekday: Int,
    openTime: LocalTime? = null,
    closeTime: LocalTime? = null,
    isClosed: Boolean = false
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "library_id", nullable = false)
    var library: Library = library
        protected set

    @Column(nullable = false)
    var weekday: Int = weekday
        protected set

    @Column(name = "open_time")
    var openTime: LocalTime? = openTime
        protected set

    @Column(name = "close_time")
    var closeTime: LocalTime? = closeTime
        protected set

    @Column(name = "is_closed", nullable = false)
    var isClosed: Boolean = isClosed
        protected set

    fun isOpenAt(targetTime: LocalTime): Boolean {
        if (isClosed) {
            return false
        }

        val start = openTime ?: return false
        val end = closeTime ?: return false

        return !targetTime.isBefore(start) && targetTime.isBefore(end)
    }
}
