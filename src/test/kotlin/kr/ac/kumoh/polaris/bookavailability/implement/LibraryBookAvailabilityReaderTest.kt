package kr.ac.kumoh.polaris.bookavailability.implement

import kr.ac.kumoh.polaris.bookavailability.implement.client.Data4LibraryBookExistClient
import kr.ac.kumoh.polaris.bookavailability.implement.client.Data4LibraryBookExistResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityStatus
import kr.ac.kumoh.polaris.global.config.CacheConfig
import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryBookAvailabilityReaderTest {
    @Test
    fun `reader caches by library code and isbn`() {
        val client = CountingData4LibraryBookExistClient()
        val reader = LibraryBookAvailabilityReader(
            cacheManager = configuredCacheManager(),
            data4LibraryBookExistClient = client
        )

        reader.read(libCode = "LIB-1", isbn = "9781234567890")
        reader.read(libCode = "LIB-1", isbn = "9781234567890")
        reader.read(libCode = "LIB-2", isbn = "9781234567890")

        assertEquals(2, client.callCount)
        assertEquals(
            listOf("LIB-1:9781234567890", "LIB-2:9781234567890"),
            client.requestKeys
        )
    }

    private class CountingData4LibraryBookExistClient : Data4LibraryBookExistClient(
        data4LibraryWebClient = org.springframework.web.reactive.function.client.WebClient.builder().baseUrl("http://example.com").build(),
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

    companion object {
        private fun configuredCacheManager(): org.springframework.cache.CacheManager =
            CacheConfig().cacheManager(
                Data4LibraryApiProperties(
                    baseUrl = "http://example.com",
                    authKey = "test"
                )
            ).also { cacheManager ->
                (cacheManager as org.springframework.cache.support.SimpleCacheManager).initializeCaches()
            }
    }
}
