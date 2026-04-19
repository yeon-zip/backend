package kr.ac.kumoh.polaris.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "core.openapi.data4library")
data class Data4LibraryApiProperties(
    val baseUrl: String,
    val authKey: String,
    val bookExistConcurrency: Int = 4,
    val bookExistCacheDuration: Duration = Duration.ofMinutes(30),
    val bookExistCacheMaxSize: Long = 1_000L,
    val proxy: Proxy = Proxy()
) {
    data class Proxy(
        val enabled: Boolean = false,
        val host: String = "",
        val port: Int = 8888
    )
}
