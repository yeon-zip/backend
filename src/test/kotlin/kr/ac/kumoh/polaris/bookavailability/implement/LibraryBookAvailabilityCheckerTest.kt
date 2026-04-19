package kr.ac.kumoh.polaris.bookavailability.implement

import kr.ac.kumoh.polaris.bookavailability.implement.client.Data4LibraryBookExistClient
import kr.ac.kumoh.polaris.bookavailability.implement.client.Data4LibraryBookExistResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityStatus
import kr.ac.kumoh.polaris.global.config.CacheConfig
import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LibraryBookAvailabilityCheckerTest {
    @Test
    fun `checkAll respects configured concurrency`() {
        BlockingData4LibraryBookExistClient.maxInflight.set(0)
        val release = CountDownLatch(1)
        val started = CountDownLatch(2)
        val checker = LibraryBookAvailabilityChecker(
            libraryBookAvailabilityReader = newReader(BlockingData4LibraryBookExistClient(release, started)),
            properties = testProperties(bookExistConcurrency = 2)
        )
        val executor = Executors.newSingleThreadExecutor()

        try {
            val future = executor.submit<Map<String, kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityResult>> {
                checker.checkAll(listOf("L1", "L2", "L3"), "9781234567890")
            }

            assertTrue(started.await(2, TimeUnit.SECONDS), "expected two concurrent reads to start")
            release.countDown()

            val results = future.get(5, TimeUnit.SECONDS)
            assertEquals(setOf("L1", "L2", "L3"), results.keys)
            assertEquals(2, BlockingData4LibraryBookExistClient.maxInflight.get())
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `check uses cache-backed reader on repeated request path`() {
        val client = CountingData4LibraryBookExistClient()
        val checker = LibraryBookAvailabilityChecker(
            libraryBookAvailabilityReader = newReader(client),
            properties = testProperties()
        )

        checker.check(libCode = "LIB-1", isbn = "9781234567890")
        checker.check(libCode = "LIB-1", isbn = "9781234567890")

        assertEquals(1, client.callCount)
        assertEquals(listOf("LIB-1:9781234567890"), client.requestKeys)
    }

    private fun newReader(client: Data4LibraryBookExistClient): LibraryBookAvailabilityReader =
        LibraryBookAvailabilityReader(
            cacheManager = configuredCacheManager(),
            data4LibraryBookExistClient = client
        )

    private fun configuredCacheManager() =
        CacheConfig().cacheManager(testProperties()).also { cacheManager ->
            (cacheManager as SimpleCacheManager).initializeCaches()
        }

    private fun testProperties(bookExistConcurrency: Int = 4) =
        Data4LibraryApiProperties(
            baseUrl = "http://example.com",
            authKey = "test",
            bookExistConcurrency = bookExistConcurrency
        )

    private class CountingData4LibraryBookExistClient : Data4LibraryBookExistClient(
        data4LibraryWebClient = WebClient.builder().baseUrl("http://example.com").build(),
        properties = Data4LibraryApiProperties(
            baseUrl = "http://example.com",
            authKey = "test"
        )
    ) {
        var callCount: Int = 0
        val requestKeys = mutableListOf<String>()

        override fun fetchBookExist(
            libCode: String,
            isbn: String
        ): Data4LibraryBookExistResult {
            callCount += 1
            requestKeys += "$libCode:$isbn"

            return Data4LibraryBookExistResult(
                libCode = libCode,
                isbn = isbn,
                hasBook = true,
                loanAvailable = true,
                status = LibraryBookAvailabilityStatus.AVAILABLE
            )
        }
    }

    private class BlockingData4LibraryBookExistClient(
        private val release: CountDownLatch,
        private val started: CountDownLatch
    ) : Data4LibraryBookExistClient(
        data4LibraryWebClient = WebClient.builder().baseUrl("http://example.com").build(),
        properties = Data4LibraryApiProperties(
            baseUrl = "http://example.com",
            authKey = "test"
        )
    ) {
        companion object {
            val maxInflight = AtomicInteger(0)
        }

        private val inflight = AtomicInteger(0)

        override fun fetchBookExist(
            libCode: String,
            isbn: String
        ): Data4LibraryBookExistResult {
            val active = inflight.incrementAndGet()
            maxInflight.accumulateAndGet(active, ::maxOf)
            started.countDown()
            release.await(5, TimeUnit.SECONDS)
            inflight.decrementAndGet()

            return Data4LibraryBookExistResult(
                libCode = libCode,
                isbn = isbn,
                hasBook = true,
                loanAvailable = true,
                status = LibraryBookAvailabilityStatus.AVAILABLE
            )
        }
    }
}
