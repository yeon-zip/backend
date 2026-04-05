package kr.ac.kumoh.polestar.library.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class GeoPoint(
    latitude: Double? = null,
    longitude: Double? = null
) {
    @Column(name = "latitude")
    var latitude: Double? = latitude
        protected set

    @Column(name = "longitude")
    var longitude: Double? = longitude
        protected set
}
