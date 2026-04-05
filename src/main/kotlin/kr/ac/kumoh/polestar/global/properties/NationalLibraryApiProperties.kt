package kr.ac.kumoh.polestar.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "core.openapi.national-library")
data class NationalLibraryApiProperties(
    val baseUrl: String,
    val apiKey: String
)
