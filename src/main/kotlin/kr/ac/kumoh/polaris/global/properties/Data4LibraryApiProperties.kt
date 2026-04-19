package kr.ac.kumoh.polaris.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "core.openapi.data4library")
data class Data4LibraryApiProperties(
    val baseUrl: String,
    val authKey: String,
    val bookExistConcurrency: Int = 8,
    val bookExistCacheDuration: Duration = Duration.ofHours(12),
    val bookExistCacheMaxSize: Long = 1_000L
)
