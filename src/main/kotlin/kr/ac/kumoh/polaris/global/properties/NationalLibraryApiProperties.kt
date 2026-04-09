package kr.ac.kumoh.polaris.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "core.openapi.national-library")
data class NationalLibraryApiProperties(
    val baseUrl: String,
    val apiKey: String
)
