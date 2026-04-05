package kr.ac.kumoh.polestar.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "core.openapi.data4library")
data class Data4LibraryApiProperties(
    val baseUrl: String,
    val authKey: String
)
