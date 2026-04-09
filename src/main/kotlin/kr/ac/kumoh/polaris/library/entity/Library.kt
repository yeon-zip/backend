package kr.ac.kumoh.polaris.library.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "library")
class Library(
    name: String,
    address: Address = Address(),
    libCode: String,
    regionCode: String? = null,
    detailRegionCode: String? = null,
    location: GeoPoint = GeoPoint(),
    contactInfo: ContactInfo = ContactInfo(),
    rawScheduleInfo: RawScheduleInfo = RawScheduleInfo()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "name", nullable = false, length = 200)
    var name: String = name
        protected set

    @Column(name = "lib_code", nullable = false, unique = true, length = 30)
    var libCode: String = libCode
        protected set

    @Embedded
    var address: Address = address
        protected set

    @Column(name = "region_code", length = 20)
    var regionCode: String? = regionCode
        protected set

    @Column(name = "detail_region_code", length = 20)
    var detailRegionCode: String? = detailRegionCode
        protected set

    @Embedded
    var location: GeoPoint = location
        protected set

    @Embedded
    var contactInfo: ContactInfo = contactInfo
        protected set

    @Embedded
    var rawScheduleInfo: RawScheduleInfo = rawScheduleInfo
        protected set

    // ########## //

    @OneToMany(
        mappedBy = "library",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    protected val _operatingHours: MutableList<LibraryOperatingHour> = mutableListOf()

    val operatingHours: List<LibraryOperatingHour>
        get() = _operatingHours.toList()

    @OneToMany(
        mappedBy = "library",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    protected val _closedRules: MutableList<LibraryClosedRule> = mutableListOf()

    val closedRules: List<LibraryClosedRule>
        get() = _closedRules.toList()

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    fun updateFromExternal(
        name: String,
        address: Address,
        regionCode: String?,
        detailRegionCode: String?,
        location: GeoPoint,
        contactInfo: ContactInfo,
        rawScheduleInfo: RawScheduleInfo
    ) {
        this.name = name
        this.address = address
        this.regionCode = regionCode
        this.detailRegionCode = detailRegionCode
        this.location = location
        this.contactInfo = contactInfo
        this.rawScheduleInfo = rawScheduleInfo
        this.updatedAt = LocalDateTime.now()
    }
}
