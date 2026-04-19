package kr.ac.kumoh.polaris.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "core.openapi.data4library")
data class Data4LibraryApiProperties(
    val baseUrl: String,
    val authKey: String,
    val bookExistConcurrency: Int = 8
)
