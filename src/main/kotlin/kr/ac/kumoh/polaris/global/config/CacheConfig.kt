package kr.ac.kumoh.polaris.global.config

import com.github.benmanes.caffeine.cache.Caffeine
import kr.ac.kumoh.polaris.bookavailability.implement.LibraryBookAvailabilityReader
import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {
    fun buildCache(cacheName: String, duration: Duration, allowNullValue: Boolean, maxSize: Long = 1000L) =
        CaffeineCache(
            cacheName,
            Caffeine.newBuilder()
                .expireAfterWrite(duration)
                .maximumSize(maxSize)
                .build(),
            allowNullValue
        )

    @Bean
    fun cacheManager(properties: Data4LibraryApiProperties): CacheManager =
        SimpleCacheManager().apply {
            setCaches(
                setOf(
                    buildCache(
                        cacheName = LibraryBookAvailabilityReader.CACHE_NAME,
                        duration = properties.bookExistCacheDuration,
                        allowNullValue = false,
                        maxSize = properties.bookExistCacheMaxSize
                    ),
                )
            )
        }
}
