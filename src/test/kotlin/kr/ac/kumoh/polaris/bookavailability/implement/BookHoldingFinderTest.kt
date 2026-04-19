package kr.ac.kumoh.polaris.bookavailability.implement

import kr.ac.kumoh.polaris.bookavailability.implement.dto.BookHoldingCursor
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityStatus
import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import kr.ac.kumoh.polaris.library.implement.LibraryOpenNowStatus
import kr.ac.kumoh.polaris.library.implement.LibraryOpenStatusResolver
import kr.ac.kumoh.polaris.library.implement.NearbyLibraryQueryRepository
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryCursor
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryQueryResult
import org.mockito.Mockito.mock
import jakarta.persistence.EntityManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BookHoldingFinderTest {
    @Test
    fun `incremental scan stops after limit plus one visible matches and prefilters openNow`() {
        val nearbyPages = listOf(
            listOf(
                nearbyLibrary(1L, "A", 1, 1000),
                nearbyLibrary(2L, "B", 2, 2000),
                nearbyLibrary(3L, "C", 3, 3000)
            ),
            listOf(
                nearbyLibrary(4L, "D", 4, 4000),
                nearbyLibrary(5L, "E", 5, 5000),
                nearbyLibrary(6L, "F", 6, 6000)
            )
        )
        val nearbyRepository = FakeNearbyLibraryQueryRepository(nearbyPages)
        val openStatusResolver = FakeLibraryOpenStatusResolver(
            openNowByLibraryId = mapOf(
                1L to false,
                2L to false,
                3L to true,
                4L to true,
                5L to true,
                6L to true
            )
        )
        val checker = FakeLibraryBookAvailabilityChecker(
            resultsByLibCode = mapOf(
                "C" to availability(hasBook = true, loanAvailable = false),
                "D" to availability(hasBook = true, loanAvailable = true),
                "E" to availability(hasBook = true, loanAvailable = true),
                "F" to availability(hasBook = true, loanAvailable = true)
            )
        )
        val finder = BookHoldingFinder(
            nearbyLibraryQueryRepository = nearbyRepository,
            libraryBookAvailabilityChecker = checker,
            libraryOpenStatusResolver = openStatusResolver
        )

        val results = finder.findVisibleByIsbn(
            isbn = "9781234567890",
            latitude = 36.0,
            longitude = 128.0,
            radiusKm = 5.0,
            loanAvailable = true,
            openNow = true,
            cursor = null,
            limit = 3,
            now = java.time.LocalDateTime.of(2026, 4, 19, 12, 0)
        )

        assertEquals(listOf(4L, 5L, 6L), results.map { it.libraryId })
        assertEquals(listOf("C", "D", "E", "F"), checker.requestedLibCodes)
        assertEquals(2, nearbyRepository.calls.size)
        assertEquals(listOf(1L, 2L, 3L), openStatusResolver.requests[0])
        assertEquals(listOf(4L, 5L, 6L), openStatusResolver.requests[1])
        assertTrue(results.all { it.openNow })
    }

    @Test
    fun `cursor resumes after the last visible item without rescanning earlier pages`() {
        val nearbyPages = listOf(
            listOf(
                nearbyLibrary(4L, "D", 4, 4000),
                nearbyLibrary(5L, "E", 5, 5000),
                nearbyLibrary(6L, "F", 6, 6000)
            )
        )
        val nearbyRepository = FakeNearbyLibraryQueryRepository(nearbyPages)
        val openStatusResolver = FakeLibraryOpenStatusResolver(
            openNowByLibraryId = mapOf(
                4L to true,
                5L to true,
                6L to true
            )
        )
        val checker = FakeLibraryBookAvailabilityChecker(
            resultsByLibCode = mapOf(
                "D" to availability(hasBook = true, loanAvailable = true),
                "E" to availability(hasBook = true, loanAvailable = true),
                "F" to availability(hasBook = true, loanAvailable = true)
            )
        )
        val finder = BookHoldingFinder(
            nearbyLibraryQueryRepository = nearbyRepository,
            libraryBookAvailabilityChecker = checker,
            libraryOpenStatusResolver = openStatusResolver
        )

        val results = finder.findVisibleByIsbn(
            isbn = "9781234567890",
            latitude = 36.0,
            longitude = 128.0,
            radiusKm = 5.0,
            loanAvailable = true,
            openNow = true,
            cursor = BookHoldingCursor(distanceKm = 5.0, libraryId = 5L),
            limit = 2,
            now = java.time.LocalDateTime.of(2026, 4, 19, 12, 0)
        )

        assertEquals(listOf(6L), results.map { it.libraryId })
        val cursorUsed = assertNotNull(nearbyRepository.calls.single())
        assertEquals(NearbyLibraryCursor(distanceMeter = 5000L, libraryId = 5L), cursorUsed)
        assertEquals(listOf("F"), checker.requestedLibCodes)
    }

    private fun nearbyLibrary(
        libraryId: Long,
        libCode: String,
        order: Int,
        distanceMeter: Long
    ) = NearbyLibraryQueryResult(
        libraryId = libraryId,
        libCode = libCode,
        name = "Library $libCode",
        address = "Address $libCode",
        latitude = 36.0 + order,
        longitude = 128.0 + order,
        homepageUrl = null,
        tel = null,
        distanceKm = distanceMeter / 1000.0,
        distanceMeter = distanceMeter
    )

    private fun availability(
        hasBook: Boolean?,
        loanAvailable: Boolean?
    ) = LibraryBookAvailabilityResult(
        hasBook = hasBook,
        loanAvailable = loanAvailable,
        status = when (hasBook) {
            true -> LibraryBookAvailabilityStatus.AVAILABLE
            false -> LibraryBookAvailabilityStatus.UNAVAILABLE
            null -> LibraryBookAvailabilityStatus.UNKNOWN
        }
    )

    private class FakeNearbyLibraryQueryRepository(
        private val pages: List<List<NearbyLibraryQueryResult>>
    ) : NearbyLibraryQueryRepository(entityManager = mock(EntityManager::class.java)) {
        val calls = mutableListOf<NearbyLibraryCursor?>()
        private var pageIndex: Int = 0

        override fun findPageWithinRadius(
            latitude: Double,
            longitude: Double,
            radiusKm: Double,
            cursor: NearbyLibraryCursor?,
            limit: Int
        ): List<NearbyLibraryQueryResult> {
            calls += cursor
            val page = pages.getOrNull(pageIndex).orEmpty()
            pageIndex += 1

            return page.filter { nearbyLibrary ->
                cursor == null ||
                    nearbyLibrary.distanceMeter > cursor.distanceMeter ||
                    (nearbyLibrary.distanceMeter == cursor.distanceMeter && nearbyLibrary.libraryId > cursor.libraryId)
            }.take(limit)
        }
    }

    private class FakeLibraryOpenStatusResolver(
        private val openNowByLibraryId: Map<Long, Boolean>
    ) : LibraryOpenStatusResolver(
        libraryOperatingHourReader = mock(kr.ac.kumoh.polaris.library.implement.LibraryOperatingHourReader::class.java),
        libraryClosedRuleReader = mock(kr.ac.kumoh.polaris.library.implement.LibraryClosedRuleReader::class.java),
        publicHolidayReader = mock(kr.ac.kumoh.polaris.library.implement.PublicHolidayReader::class.java),
        libraryAvailabilityChecker = mock(kr.ac.kumoh.polaris.library.implement.LibraryAvailabilityChecker::class.java)
    ) {
        val requests = mutableListOf<List<Long>>()

        override fun getOpenNowStatuses(
            libraryIds: Collection<Long>,
            now: java.time.LocalDateTime
        ): List<LibraryOpenNowStatus> {
            val requestedIds = libraryIds.toList()
            requests += requestedIds

            return requestedIds.map { libraryId ->
                LibraryOpenNowStatus(
                    libraryId = libraryId,
                    openNow = openNowByLibraryId[libraryId] ?: false
                )
            }
        }
    }

    private class FakeLibraryBookAvailabilityChecker(
        private val resultsByLibCode: Map<String, LibraryBookAvailabilityResult>
    ) : LibraryBookAvailabilityChecker(
        libraryBookAvailabilityReader = LibraryBookAvailabilityReader(
            cacheManager = configuredCacheManager(),
            data4LibraryBookExistClient = FakeData4LibraryBookExistClient()
        ),
        properties = Data4LibraryApiProperties(
            baseUrl = "http://example.com",
            authKey = "test"
        )
    ) {
        val requestedLibCodes = mutableListOf<String>()

        override fun checkAll(
            libCodes: Collection<String>,
            isbn: String
        ): Map<String, LibraryBookAvailabilityResult> {
            requestedLibCodes += libCodes
            return libCodes.associateWith { libCode ->
                resultsByLibCode[libCode] ?: LibraryBookAvailabilityResult.unknown()
            }
        }
    }

    private class FakeData4LibraryBookExistClient : kr.ac.kumoh.polaris.bookavailability.implement.client.Data4LibraryBookExistClient(
        data4LibraryWebClient = org.springframework.web.reactive.function.client.WebClient.builder().baseUrl("http://example.com").build(),
        properties = Data4LibraryApiProperties(
            baseUrl = "http://example.com",
            authKey = "test"
        )
    ) {
        override fun fetchBookExist(
            libCode: String,
            isbn: String
        ) = throw UnsupportedOperationException()
    }

    companion object {
        private fun configuredCacheManager(): org.springframework.cache.CacheManager =
            kr.ac.kumoh.polaris.global.config.CacheConfig().cacheManager().also { cacheManager ->
                (cacheManager as org.springframework.cache.support.SimpleCacheManager).initializeCaches()
            }
    }
}
