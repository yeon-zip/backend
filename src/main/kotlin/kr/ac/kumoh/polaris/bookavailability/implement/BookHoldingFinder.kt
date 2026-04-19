package kr.ac.kumoh.polaris.bookavailability.implement

import kr.ac.kumoh.polaris.bookavailability.implement.dto.BookHoldingCursor
import kr.ac.kumoh.polaris.bookavailability.implement.dto.BookHoldingItemResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityResult
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.library.implement.LibraryOpenStatusResolver
import kr.ac.kumoh.polaris.library.implement.NearbyLibraryQueryRepository
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryCursor
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryQueryResult
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.math.roundToLong

@Component
class BookHoldingFinder(
    private val nearbyLibraryQueryRepository: NearbyLibraryQueryRepository,
    private val libraryBookAvailabilityChecker: LibraryBookAvailabilityChecker,
    private val libraryOpenStatusResolver: LibraryOpenStatusResolver
) {
    fun findVisibleByIsbn(
        isbn: String,
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        loanAvailable: Boolean?,
        openNow: Boolean?,
        cursor: BookHoldingCursor?,
        limit: Int,
        now: LocalDateTime
    ): List<BookHoldingItemResult> {
        if (radiusKm <= 0) {
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        }

        val visibleHoldings = mutableListOf<ScannedHolding>()
        var nearbyCursor = cursor?.toNearbyCursor()

        while (visibleHoldings.size < limit) {
            val nearbyLibraries = nearbyLibraryQueryRepository.findPageWithinRadius(
                latitude = latitude,
                longitude = longitude,
                radiusKm = radiusKm,
                cursor = nearbyCursor,
                limit = limit
            )
            if (nearbyLibraries.isEmpty()) {
                break
            }

            nearbyCursor = nearbyLibraries.last().toNearbyCursor()

            val openNowStatuses = if (openNow == null) {
                emptyMap()
            } else {
                resolveOpenNowStatuses(
                    nearbyLibraries = nearbyLibraries,
                    now = now
                )
            }
            val visibleCandidates = nearbyLibraries.filter { nearbyLibrary ->
                openNow == null || openNowStatuses[nearbyLibrary.libraryId] == openNow
            }

            if (visibleCandidates.isEmpty()) {
                continue
            }

            val availabilityByLibCode = libraryBookAvailabilityChecker.checkAll(
                libCodes = visibleCandidates.map { it.libCode },
                isbn = isbn
            )

            for (nearbyLibrary in visibleCandidates) {
                val availability = availabilityByLibCode[nearbyLibrary.libCode]
                    ?: LibraryBookAvailabilityResult.unknown()

                if (availability.hasBook == false) {
                    continue
                }
                if (loanAvailable != null && availability.loanAvailable != loanAvailable) {
                    continue
                }

                visibleHoldings += ScannedHolding(
                    nearbyLibrary = nearbyLibrary,
                    availability = availability,
                    openNow = openNowStatuses[nearbyLibrary.libraryId]
                )

                if (visibleHoldings.size == limit) {
                    break
                }
            }
        }

        val resolvedOpenNowStatuses = resolveMissingOpenNowStatuses(
            visibleHoldings = visibleHoldings,
            now = now
        )

        return visibleHoldings.map { holding ->
            holding.toBookHoldingItemResult(
                openNow = holding.openNow ?: resolvedOpenNowStatuses[holding.nearbyLibrary.libraryId] ?: false
            )
        }
    }

    private fun resolveOpenNowStatuses(
        nearbyLibraries: List<NearbyLibraryQueryResult>,
        now: LocalDateTime
    ): Map<Long, Boolean> =
        libraryOpenStatusResolver.getOpenNowStatuses(
            libraryIds = nearbyLibraries.map { it.libraryId },
            now = now
        ).associate { status ->
            status.libraryId to status.openNow
        }

    private fun resolveMissingOpenNowStatuses(
        visibleHoldings: List<ScannedHolding>,
        now: LocalDateTime
    ): Map<Long, Boolean> {
        val unresolvedLibraryIds = visibleHoldings.mapNotNull { holding ->
            if (holding.openNow == null) {
                holding.nearbyLibrary.libraryId
            } else {
                null
            }
        }
        if (unresolvedLibraryIds.isEmpty()) {
            return emptyMap()
        }

        return libraryOpenStatusResolver.getOpenNowStatuses(
            libraryIds = unresolvedLibraryIds,
            now = now
        ).associate { status ->
            status.libraryId to status.openNow
        }
    }

    private fun BookHoldingCursor.toNearbyCursor(): NearbyLibraryCursor =
        NearbyLibraryCursor(
            distanceMeter = (distanceKm * 1000.0).roundToLong(),
            libraryId = libraryId
        )

    private fun NearbyLibraryQueryResult.toNearbyCursor(): NearbyLibraryCursor =
        NearbyLibraryCursor(
            distanceMeter = distanceMeter,
            libraryId = libraryId
        )

    private data class ScannedHolding(
        val nearbyLibrary: NearbyLibraryQueryResult,
        val availability: LibraryBookAvailabilityResult,
        val openNow: Boolean?
    ) {
        fun toBookHoldingItemResult(openNow: Boolean): BookHoldingItemResult =
            BookHoldingItemResult(
                libraryId = nearbyLibrary.libraryId,
                name = nearbyLibrary.name,
                address = nearbyLibrary.address,
                latitude = nearbyLibrary.latitude,
                longitude = nearbyLibrary.longitude,
                distanceKm = nearbyLibrary.distanceKm,
                hasBook = availability.hasBook,
                loanAvailable = availability.loanAvailable,
                availabilityStatus = availability.status,
                openNow = openNow
            )
    }
}
