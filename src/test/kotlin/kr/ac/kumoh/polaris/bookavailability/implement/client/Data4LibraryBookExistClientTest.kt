package kr.ac.kumoh.polaris.bookavailability.implement.client

import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityStatus
import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(OutputCaptureExtension::class)
class Data4LibraryBookExistClientTest {
    @Test
    fun `returns unknown and logs xml api error body`(output: CapturedOutput) {
        val client = newClient(
            status = HttpStatus.OK,
            contentType = MediaType.APPLICATION_XML,
            body = """
                <response>
                    <error>1일 500건 이상 요청 시 IP 등록이 필요합니다. 등록 된 IP를 확인하시기 바랍니다.</error>
                </response>
            """.trimIndent()
        )

        val result = client.fetchBookExist(libCode = "LIB-1", isbn = "9781234567890")

        assertEquals(LibraryBookAvailabilityStatus.UNKNOWN, result.status)
        assertNull(result.hasBook)
        assertNull(result.loanAvailable)
        assert(output.out.contains("도서 소장 여부 조회 API 오류 응답"))
        assert(output.out.contains("1일 500건 이상 요청 시 IP 등록이 필요합니다. 등록 된 IP를 확인하시기 바랍니다."))
    }

    @Test
    fun `returns unknown and logs json api error body`(output: CapturedOutput) {
        val client = newClient(
            status = HttpStatus.OK,
            contentType = MediaType.APPLICATION_JSON,
            body = """
                {
                  "response": {
                    "error": "1일 500건 이상 요청 시 IP 등록이 필요합니다. 등록 된 IP를 확인하시기 바랍니다."
                  }
                }
            """.trimIndent()
        )

        val result = client.fetchBookExist(libCode = "LIB-1", isbn = "9781234567890")

        assertEquals(LibraryBookAvailabilityStatus.UNKNOWN, result.status)
        assertNull(result.hasBook)
        assertNull(result.loanAvailable)
        assert(output.out.contains("도서 소장 여부 조회 API 오류 응답"))
        assert(output.out.contains("1일 500건 이상 요청 시 IP 등록이 필요합니다. 등록 된 IP를 확인하시기 바랍니다."))
    }

    @Test
    fun `logs unexpected payload when response body has no usable fields`(output: CapturedOutput) {
        val client = newClient(
            status = HttpStatus.OK,
            contentType = MediaType.APPLICATION_JSON,
            body = """
                {
                  "response": {
                    "unexpected": true
                  }
                }
            """.trimIndent()
        )

        val result = client.fetchBookExist(libCode = "LIB-1", isbn = "9781234567890")

        assertEquals(LibraryBookAvailabilityStatus.UNKNOWN, result.status)
        assert(output.out.contains("도서 소장 여부 조회 비정상 응답"))
    }

    @Test
    fun `parses successful json response`() {
        val client = newClient(
            status = HttpStatus.OK,
            contentType = MediaType.APPLICATION_JSON,
            body = """
                {
                  "response": {
                    "resultNum": 1,
                    "result": {
                      "hasBook": "Y",
                      "loanAvailable": "N"
                    }
                  }
                }
            """.trimIndent()
        )

        val result = client.fetchBookExist(libCode = "LIB-1", isbn = "9781234567890")

        assertEquals(LibraryBookAvailabilityStatus.ON_LOAN, result.status)
        assertEquals(true, result.hasBook)
        assertEquals(false, result.loanAvailable)
    }

    @Test
    fun `maps available when book exists and loan is available`() {
        val client = newClient(
            status = HttpStatus.OK,
            contentType = MediaType.APPLICATION_JSON,
            body = """
                {
                  "response": {
                    "resultNum": 1,
                    "result": {
                      "hasBook": "Y",
                      "loanAvailable": "Y"
                    }
                  }
                }
            """.trimIndent()
        )

        val result = client.fetchBookExist(libCode = "LIB-1", isbn = "9781234567890")

        assertEquals(LibraryBookAvailabilityStatus.AVAILABLE, result.status)
        assertEquals(true, result.hasBook)
        assertEquals(true, result.loanAvailable)
    }

    private fun newClient(
        status: HttpStatus,
        contentType: MediaType,
        body: String
    ): Data4LibraryBookExistClient {
        val webClient = WebClient.builder()
            .baseUrl("http://example.com")
            .exchangeFunction {
                Mono.just(
                    ClientResponse.create(status)
                        .header(HttpHeaders.CONTENT_TYPE, contentType.toString())
                        .body(body)
                        .build()
                )
            }
            .build()

        return Data4LibraryBookExistClient(
            data4LibraryWebClient = webClient,
            properties = Data4LibraryApiProperties(
                baseUrl = "http://example.com",
                authKey = "test"
            )
        )
    }
}
