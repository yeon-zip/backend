package kr.ac.kumoh.polaris.library.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class RawScheduleInfo(
    operatingTime: String? = null,
    closedInfo: String? = null
) {
    @Column(name = "operating_time_raw", columnDefinition = "TEXT")
    var operatingTime: String? = operatingTime
        protected set

    @Column(name = "closed_raw", columnDefinition = "TEXT")
    var closedInfo: String? = closedInfo
        protected set
}
