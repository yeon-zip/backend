package kr.ac.kumoh.polaris.bookavailability.service

import kr.ac.kumoh.polaris.bookavailability.implement.BookHoldingFinder
import kr.ac.kumoh.polaris.bookavailability.implement.dto.BookHoldingCursor
import kr.ac.kumoh.polaris.bookavailability.implement.dto.BookHoldingItemResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.LocalDateTime

class BookAvailabilityServiceTest {
    @Test
    fun `getBookHoldings slices limit plus one results into page metadata`() {
        val finder = RecordingBookHoldingFinder(
            items = listOf(
                holding(1L, 1.0),
                holding(2L, 2.0),
                holding(3L, 3.0)
            )
        )
        val service = BookAvailabilityService(bookHoldingFinder = finder)

        val result = service.getBookHoldings(
            isbn = "9781234567890",
            latitude = 36.0,
            longitude = 128.0,
            radiusKm = 5.0,
            loanAvailable = true,
            openNow = true,
            cursor = "1.0:1",
            limit = 2
        )

        assertEquals(listOf(1L, 2L), result.items.map { it.libraryId })
        assertTrue(result.hasNext)
        assertEquals("2.0:2", result.nextCursor)
        assertEquals(3, finder.requestedLimit)
        assertEquals(BookHoldingCursor(distanceKm = 1.0, libraryId = 1L), finder.requestedCursor)
    }

    private fun holding(
        libraryId: Long,
        distanceKm: Double
    ) = BookHoldingItemResult(
        libraryId = libraryId,
        name = "Library-$libraryId",
        address = "Address-$libraryId",
        latitude = 36.0,
        longitude = 128.0,
        distanceKm = distanceKm,
        hasBook = true,
        loanAvailable = true,
        availabilityStatus = LibraryBookAvailabilityStatus.AVAILABLE,
        openNow = true
    )

    private class RecordingBookHoldingFinder(
        private val items: List<BookHoldingItemResult>
    ) : BookHoldingFinder(
        nearbyLibraryQueryRepository = mock(),
        libraryBookAvailabilityChecker = mock(),
        libraryOpenStatusResolver = mock()
    ) {
        var requestedLimit: Int = -1
        var requestedCursor: BookHoldingCursor? = null

        override fun findVisibleByIsbn(
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
            requestedLimit = limit
            requestedCursor = cursor
            return items
        }
    }
}
