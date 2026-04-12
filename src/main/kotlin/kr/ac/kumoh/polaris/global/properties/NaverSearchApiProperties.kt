package kr.ac.kumoh.polaris.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "core.openapi.naver.search")
data class NaverSearchApiProperties(
    val list: NaverSearchApiDetailProperties,
    val detail: NaverSearchApiDetailProperties
) {
    companion object {
        const val HEADER_CLIENT_ID: String = "X-Naver-Client-Id"
        const val HEADER_CLIENT_SECRET: String = "X-Naver-Client-Secret"
    }
}

data class NaverSearchApiDetailProperties(
    val baseUrl: String,
    val clientId: String,
    val clientSecret: String
)
