package kr.ac.kumoh.polaris.global.config

import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import kr.ac.kumoh.polaris.global.properties.NaverSearchApiProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class RestClientConfig {
    @Bean
    fun restClientBuilder(): RestClient.Builder {
        return RestClient.builder()
    }

    @Bean
    fun data4LibraryRestClient(
        builder: RestClient.Builder,
        properties: Data4LibraryApiProperties
    ): RestClient =
        builder
            .baseUrl(properties.baseUrl)
            .build()

    @Bean
    fun data4LibraryWebClient(
        builder: WebClient.Builder,
        properties: Data4LibraryApiProperties
    ): WebClient =
        builder
            .baseUrl(properties.baseUrl)
            .build()

    @Bean
    fun naverSearchListRestClient(
        builder: RestClient.Builder,
        properties: NaverSearchApiProperties
    ): RestClient =
        builder
            .baseUrl(properties.list.baseUrl)
            .build()

    @Bean
    fun naverSearchDetailRestClient(
        builder: RestClient.Builder,
        properties: NaverSearchApiProperties
    ): RestClient =
        builder
            .baseUrl(properties.detail.baseUrl)
            .build()
}
