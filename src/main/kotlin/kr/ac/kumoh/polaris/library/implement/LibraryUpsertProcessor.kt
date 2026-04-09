package kr.ac.kumoh.polaris.library.implement

import kr.ac.kumoh.polaris.library.entity.Address
import kr.ac.kumoh.polaris.library.entity.ContactInfo
import kr.ac.kumoh.polaris.library.entity.GeoPoint
import kr.ac.kumoh.polaris.library.entity.Library
import kr.ac.kumoh.polaris.library.entity.RawScheduleInfo
import kr.ac.kumoh.polaris.library.implement.client.dto.Data4LibraryLibrary
import kr.ac.kumoh.polaris.library.repository.LibraryRepository
import org.springframework.stereotype.Component

@Component
class LibraryUpsertProcessor(
    private val libraryRepository: LibraryRepository
) {
    fun upsert(document: Data4LibraryLibrary): Library =
        libraryRepository.findByLibCode(document.libCode)
            ?.let { existing ->
                existing.updateFromExternal(
                    name = document.libName,
                    address = document.toAddress(),
                    regionCode = document.region,
                    detailRegionCode = document.detailRegion,
                    location = document.toGeoPoint(),
                    contactInfo = document.toContactInfo(),
                    rawScheduleInfo = document.toRawScheduleInfo()
                )
                libraryRepository.save(existing)
            }
            ?: libraryRepository.save(
                Library(
                    name = document.libName,
                    libCode = document.libCode,
                    regionCode = document.region,
                    detailRegionCode = document.detailRegion,
                    address = document.toAddress(),
                    location = document.toGeoPoint(),
                    contactInfo = document.toContactInfo(),
                    rawScheduleInfo = document.toRawScheduleInfo()
                )
            )

    private fun Data4LibraryLibrary.toAddress(): Address =
        Address(province = address?.trim())

    private fun Data4LibraryLibrary.toGeoPoint(): GeoPoint =
        GeoPoint(latitude, longitude)

    private fun Data4LibraryLibrary.toContactInfo(): ContactInfo =
        ContactInfo(
            homepageUrl = homepage,
            tel = tel
        )

    private fun Data4LibraryLibrary.toRawScheduleInfo(): RawScheduleInfo =
        RawScheduleInfo(
            operatingTime = operatingTime,
            closedInfo = closed
        )
}
