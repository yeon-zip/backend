package kr.ac.kumoh.polaris.global.config

import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import kr.ac.kumoh.polaris.global.properties.NaverSearchApiProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.http.HttpClient as JdkHttpClient

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
            .applyData4LibraryProxy(properties)
            .build()

    @Bean
    fun data4LibraryWebClient(
        builder: WebClient.Builder,
        properties: Data4LibraryApiProperties
    ): WebClient =
        builder
            .baseUrl(properties.baseUrl)
            .applyData4LibraryProxy(properties)
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

    internal fun RestClient.Builder.applyData4LibraryProxy(
        properties: Data4LibraryApiProperties
    ): RestClient.Builder {
        val proxy = properties.proxy.takeIf { it.enabled } ?: return this
        val address = proxy.toAddress()

        val httpClient = JdkHttpClient.newBuilder()
            .proxy(ProxySelector.of(address))
            .build()

        return requestFactory(JdkClientHttpRequestFactory(httpClient))
    }

    internal fun WebClient.Builder.applyData4LibraryProxy(
        properties: Data4LibraryApiProperties
    ): WebClient.Builder {
        val proxy = properties.proxy.takeIf { it.enabled } ?: return this
        val address = proxy.toAddress()

        val httpClient = HttpClient.create()
            .proxy { proxySpec ->
                proxySpec
                    .type(ProxyProvider.Proxy.HTTP)
                    .host(address.hostString)
                    .port(address.port)
            }

        return clientConnector(ReactorClientHttpConnector(httpClient))
    }

    internal fun Data4LibraryApiProperties.Proxy.toAddress(): InetSocketAddress {
        require(host.isNotBlank()) {
            "data4library 프록시가 활성화되었지만 host 값이 비어 있습니다."
        }
        require(port in 1..65535) {
            "data4library 프록시 port 값이 올바르지 않습니다. port=$port"
        }

        return InetSocketAddress.createUnresolved(host, port)
    }
}
