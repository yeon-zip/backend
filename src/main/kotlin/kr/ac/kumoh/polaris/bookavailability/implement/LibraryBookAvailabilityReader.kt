package kr.ac.kumoh.polaris.bookavailability.implement

import kr.ac.kumoh.polaris.bookavailability.implement.client.Data4LibraryBookExistClient
import kr.ac.kumoh.polaris.bookavailability.implement.client.Data4LibraryBookExistResult
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class LibraryBookAvailabilityReader(
    cacheManager: CacheManager,
    private val data4LibraryBookExistClient: Data4LibraryBookExistClient
) {
    private val cache = requireNotNull(cacheManager.getCache(CACHE_NAME)) {
        "Cache not found: $CACHE_NAME"
    }

    fun read(
        libCode: String,
        isbn: String
    ): Data4LibraryBookExistResult =
        requireNotNull(
            cache.get(cacheKey(libCode, isbn)) {
                data4LibraryBookExistClient.fetchBookExist(
                    libCode = libCode,
                    isbn = isbn
                )
            }
        )

    private fun cacheKey(
        libCode: String,
        isbn: String
    ): String = "$libCode:$isbn"

    companion object {
        const val CACHE_NAME: String = "libraryBookAvailability"
    }
}
