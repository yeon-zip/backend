package kr.ac.kumoh.polaris.bookavailability.implement

import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.NearbyLibraryBookAvailability
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.library.implement.NearbyLibraryQueryRepository
import org.springframework.stereotype.Component

@Component
class BookHoldingFinder(
    private val nearbyLibraryQueryRepository: NearbyLibraryQueryRepository,
    private val libraryBookAvailabilityChecker: LibraryBookAvailabilityChecker
) {
    fun findByIsbn(
        isbn: String,
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        loanAvailable: Boolean?
    ): List<NearbyLibraryBookAvailability> {
        if (radiusKm <= 0) {
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        }

        val nearbyLibraries = nearbyLibraryQueryRepository.findWithinRadius(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm
        )
        val availabilityByLibCode = libraryBookAvailabilityChecker.checkAll(
            libCodes = nearbyLibraries.map { it.libCode },
            isbn = isbn
        )

        return nearbyLibraries.map { nearbyLibrary ->
            val availability = availabilityByLibCode[nearbyLibrary.libCode]
                ?: LibraryBookAvailabilityResult.unknown()

            NearbyLibraryBookAvailability(
                libraryId = nearbyLibrary.libraryId,
                libCode = nearbyLibrary.libCode,
                name = nearbyLibrary.name,
                address = nearbyLibrary.address,
                latitude = nearbyLibrary.latitude,
                longitude = nearbyLibrary.longitude,
                homepageUrl = nearbyLibrary.homepageUrl,
                tel = nearbyLibrary.tel,
                distanceKm = nearbyLibrary.distanceKm,
                hasBook = availability.hasBook,
                loanAvailable = availability.loanAvailable,
                status = availability.status
            )
        }.filter { holding ->
            holding.hasBook != false
        }.filter { holding ->
            loanAvailable == null || holding.loanAvailable == loanAvailable
        }
    }
}
