package kr.ac.kumoh.polestar.bookavailability.service

import kr.ac.kumoh.polestar.bookavailability.implement.BookHoldingFinder
import kr.ac.kumoh.polestar.bookavailability.implement.dto.BookHoldingCursor
import kr.ac.kumoh.polestar.bookavailability.implement.dto.BookHoldingItemResult
import kr.ac.kumoh.polestar.bookavailability.implement.dto.NearbyLibraryBookAvailability
import kr.ac.kumoh.polestar.global.dto.CursorPageResult
import kr.ac.kumoh.polestar.global.exception.ErrorCode
import kr.ac.kumoh.polestar.global.exception.ServiceException
import kr.ac.kumoh.polestar.global.util.IsbnNormalizer
import kr.ac.kumoh.polestar.library.service.LibraryInfoService
import kr.ac.kumoh.polestar.library.service.LibraryOpenNowStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BookAvailabilityService(
    private val bookHoldingFinder: BookHoldingFinder,
    private val libraryInfoService: LibraryInfoService
) {
    fun getBookHoldings(
        isbn: String,
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        loanAvailable: Boolean?,
        openNow: Boolean?,
        cursor: String?,
        limit: Int
    ): CursorPageResult<BookHoldingItemResult> {
        val normalizedIsbn = IsbnNormalizer.normalize(isbn)
        validateRequest(limit)

        val now = LocalDateTime.now()
        val holdings = buildHoldingItems(
            isbn = normalizedIsbn,
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            loanAvailable = loanAvailable,
            now = now
        )
        val filteredHoldings = applyOpenNowFilter(
            holdings = sortHoldings(holdings),
            openNow = openNow
        )

        return sliceHoldings(
            holdings = filteredHoldings,
            cursor = cursor?.let(::parseCursor),
            limit = limit
        )
    }

    fun getLibrariesByBook(
        isbn: String,
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        loanAvailable: Boolean?,
        openNow: Boolean?,
        cursor: String?,
        limit: Int
    ): CursorPageResult<BookHoldingItemResult> =
        getBookHoldings(
            isbn = isbn,
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            loanAvailable = loanAvailable,
            openNow = openNow,
            cursor = cursor,
            limit = limit
        )

    private fun validateRequest(limit: Int) {
        if (limit !in 1..100) {
            throw ServiceException(ErrorCode.INVALID_LIMIT)
        }
    }

    private fun buildHoldingItems(
        isbn: String,
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        loanAvailable: Boolean?,
        now: LocalDateTime
    ): List<BookHoldingItemResult> {
        val nearbyHoldings = bookHoldingFinder.findByIsbn(
            isbn = isbn,
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            loanAvailable = loanAvailable
        )
        val openNowStatuses = resolveOpenNowStatuses(
            holdings = nearbyHoldings,
            now = now
        )

        return applyOpenNowStatuses(
            holdings = nearbyHoldings,
            openNowStatuses = openNowStatuses
        )
    }

    private fun resolveOpenNowStatuses(
        holdings: List<NearbyLibraryBookAvailability>,
        now: LocalDateTime
    ): List<LibraryOpenNowStatus> =
        libraryInfoService.getOpenNowStatuses(
            libraryIds = holdings.map { holding ->
                requireNotNull(holding.library.id)
            },
            now = now
        )

    private fun applyOpenNowStatuses(
        holdings: List<NearbyLibraryBookAvailability>,
        openNowStatuses: List<LibraryOpenNowStatus>
    ): List<BookHoldingItemResult> {
        val openNowIndex = openNowStatuses.associate { status ->
            status.libraryId to status.openNow
        }

        return holdings.map { holding ->
            val libraryId = requireNotNull(holding.library.id)
            BookHoldingItemResult(
                libraryId = libraryId,
                name = holding.library.name,
                address = holding.library.address.toString(),
                latitude = holding.library.location.latitude,
                longitude = holding.library.location.longitude,
                distanceKm = holding.distanceKm,
                hasBook = holding.hasBook,
                loanAvailable = holding.loanAvailable,
                availabilityStatus = holding.status,
                openNow = openNowIndex[libraryId] ?: false
            )
        }
    }

    private fun sortHoldings(
        holdings: List<BookHoldingItemResult>
    ): List<BookHoldingItemResult> =
        holdings.sortedWith(
            compareBy<BookHoldingItemResult> { it.distanceKm }
                .thenBy { it.libraryId }
        )

    private fun applyOpenNowFilter(
        holdings: List<BookHoldingItemResult>,
        openNow: Boolean?
    ): List<BookHoldingItemResult> =
        holdings.filter { holding ->
            openNow == null || holding.openNow == openNow
        }

    private fun applyCursor(
        holdings: List<BookHoldingItemResult>,
        cursor: BookHoldingCursor?
    ): List<BookHoldingItemResult> {
        if (cursor == null) {
            return holdings
        }

        return holdings.filter { holding ->
            holding.distanceKm > cursor.distanceKm ||
                (holding.distanceKm == cursor.distanceKm && holding.libraryId > cursor.libraryId)
        }
    }

    private fun parseCursor(rawCursor: String): BookHoldingCursor {
        val parts = rawCursor.split(":")
        if (parts.size != 2) {
            throw ServiceException(ErrorCode.INVALID_CURSOR)
        }

        val distanceKm = parts[0].toDoubleOrNull()
            ?: throw ServiceException(ErrorCode.INVALID_CURSOR)
        val libraryId = parts[1].toLongOrNull()
            ?: throw ServiceException(ErrorCode.INVALID_CURSOR)

        return BookHoldingCursor(
            distanceKm = distanceKm,
            libraryId = libraryId
        )
    }

    private fun sliceHoldings(
        holdings: List<BookHoldingItemResult>,
        cursor: BookHoldingCursor?,
        limit: Int
    ): CursorPageResult<BookHoldingItemResult> {
        val filteredHoldings = applyCursor(holdings, cursor)
        val pageItems = filteredHoldings.take(limit)
        val hasNext = filteredHoldings.size > limit
        val nextCursor = if (hasNext) pageItems.lastOrNull()?.toCursor() else null

        return CursorPageResult(
            nextCursor = nextCursor,
            hasNext = hasNext,
            items = pageItems
        )
    }

    private fun BookHoldingItemResult.toCursor(): String = "$distanceKm:$libraryId"
}
