package kr.ac.kumoh.polaris.bookavailability.service

import kr.ac.kumoh.polaris.bookavailability.implement.BookHoldingFinder
import kr.ac.kumoh.polaris.bookavailability.implement.dto.BookHoldingCursor
import kr.ac.kumoh.polaris.bookavailability.implement.dto.BookHoldingItemResult
import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.global.util.IsbnNormalizer
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BookAvailabilityService(
    private val bookHoldingFinder: BookHoldingFinder
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
        val holdings = bookHoldingFinder.findVisibleByIsbn(
            isbn = normalizedIsbn,
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            loanAvailable = loanAvailable,
            openNow = openNow,
            cursor = cursor?.let(::parseCursor),
            limit = limit + 1,
            now = now
        )
        val pageItems = if (holdings.size > limit) holdings.take(limit) else holdings
        val hasNext = holdings.size > limit

        return CursorPageResult(
            nextCursor = if (hasNext) pageItems.lastOrNull()?.toCursor() else null,
            hasNext = hasNext,
            items = pageItems
        )
    }

    private fun parseCursor(rawCursor: String): BookHoldingCursor {
        val parts = rawCursor.split(":")
        if (parts.size != 2) {
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        }

        val distanceKm = parts[0].toDoubleOrNull()
            ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        val libraryId = parts[1].toLongOrNull()
            ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)

        return BookHoldingCursor(
            distanceKm = distanceKm,
            libraryId = libraryId
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
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        }
    }

    private fun BookHoldingItemResult.toCursor(): String = "$distanceKm:$libraryId"
}
