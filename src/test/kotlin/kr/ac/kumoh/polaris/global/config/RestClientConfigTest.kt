package kr.ac.kumoh.polaris.global.config

import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class RestClientConfigTest {
    private val config = RestClientConfig()

    @Test
    fun `프록시가 활성화되면 설정된 주소를 사용한다`() {
        val proxy = Data4LibraryApiProperties.Proxy(
            enabled = true,
            host = "100.124.125.94",
            port = 8888
        )

        val address = with(config) { proxy.toAddress() }

        assertEquals("100.124.125.94", address.hostString)
        assertEquals(8888, address.port)
    }

    @Test
    fun `프록시 host 없이 활성화하면 예외가 발생한다`() {
        val proxy = Data4LibraryApiProperties.Proxy(
            enabled = true,
            host = "",
            port = 8888
        )

        assertFailsWith<IllegalArgumentException> {
            with(config) { proxy.toAddress() }
        }
    }

    @Test
    fun `프록시가 활성화되어도 data4library 클라이언트를 생성할 수 있다`() {
        val properties = Data4LibraryApiProperties(
            baseUrl = "http://data4library.kr",
            authKey = "auth-key",
            proxy = Data4LibraryApiProperties.Proxy(
                enabled = true,
                host = "100.124.125.94",
                port = 8888
            )
        )

        val webClient = config.data4LibraryWebClient(WebClient.builder(), properties)
        val restClient = config.data4LibraryRestClient(RestClient.builder(), properties)

        assertNotNull(webClient)
        assertNotNull(restClient)
    }
}
