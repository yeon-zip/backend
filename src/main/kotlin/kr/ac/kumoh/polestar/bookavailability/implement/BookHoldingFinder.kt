package kr.ac.kumoh.polestar.bookavailability.implement

import kr.ac.kumoh.polestar.bookavailability.implement.dto.LibraryBookAvailabilityResult
import kr.ac.kumoh.polestar.bookavailability.implement.dto.NearbyLibraryBookAvailability
import kr.ac.kumoh.polestar.global.exception.ErrorCode
import kr.ac.kumoh.polestar.global.exception.ServiceException
import kr.ac.kumoh.polestar.library.implement.NearbyLibraryFinder
import org.springframework.stereotype.Component

@Component
class BookHoldingFinder(
    private val nearbyLibraryFinder: NearbyLibraryFinder,
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
            throw ServiceException(ErrorCode.INVALID_RADIUS_KM)
        }

        val nearbyLibraries = nearbyLibraryFinder.findWithinRadius(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm
        )
        val availabilityByLibCode = libraryBookAvailabilityChecker.checkAll(
            libCodes = nearbyLibraries.map { it.library.libCode },
            isbn = isbn
        )

        return nearbyLibraries.map { libraryWithDistance ->
            val availability = availabilityByLibCode[libraryWithDistance.library.libCode]
                ?: LibraryBookAvailabilityResult.unknown()

            NearbyLibraryBookAvailability(
                library = libraryWithDistance.library,
                distanceKm = libraryWithDistance.distanceKm,
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
