package kr.ac.kumoh.polestar.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "core.openapi.naver.search")
data class NaverSearchApiProperties(
    val baseUrl: String,
    val clientId: String,
    val clientSecret: String
)
