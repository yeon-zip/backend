package kr.ac.kumoh.polestar.bookavailability.implement.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kr.ac.kumoh.polestar.bookavailability.implement.dto.LibraryBookAvailabilityStatus
import kr.ac.kumoh.polestar.global.properties.Data4LibraryApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class Data4LibraryBookExistClient(
    private val data4LibraryWebClient: WebClient,
    private val properties: Data4LibraryApiProperties
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun fetchBookExist(
        libCode: String,
        isbn: String
    ): Data4LibraryBookExistResult =
        fetchBookExistAsync(
            libCode = libCode,
            isbn = isbn
        ).blockOptional()
            .orElseGet { Data4LibraryBookExistResult.unknown(libCode, isbn) }

    fun fetchBookExistAsync(
        libCode: String,
        isbn: String
    ): Mono<Data4LibraryBookExistResult> {
        val startedAt = System.nanoTime()

        return data4LibraryWebClient.get()
            .uri { builder ->
                builder.path("/api/bookExist")
                    .queryParam("authKey", properties.authKey)
                    .queryParam("libCode", libCode)
                    .queryParam("isbn13", isbn)
                    .queryParam("format", "json")
                    .build()
            }
            .retrieve()
            .bodyToMono(Data4LibraryBookExistResponse::class.java)
            .map { response ->
                toResult(
                    libCode = libCode,
                    isbn = isbn,
                    response = response
                )
            }
            .switchIfEmpty(
                Mono.fromSupplier {
                    log.warn("도서 소장 여부 응답이 비어 있습니다 - libCode={}, isbn={}", libCode, isbn)
                    Data4LibraryBookExistResult.unknown(libCode, isbn)
                }
            )
            .doOnSubscribe {
                log.debug("도서 소장 여부 조회 요청 시작 - libCode={}, isbn={}", libCode, isbn)
            }
            .doOnNext { result ->
                log.debug(
                    "도서 소장 여부 조회 완료 - libCode={}, isbn={}, hasBook={}, loanAvailable={}, status={}, elapsedMs={}",
                    libCode,
                    isbn,
                    result.hasBook,
                    result.loanAvailable,
                    result.status,
                    elapsedMillis(startedAt)
                )
            }
            .onErrorResume { exception ->
                log.error("도서 소장 여부 조회 실패 - libCode={}, isbn={}", libCode, isbn, exception)
                Mono.just(Data4LibraryBookExistResult.unknown(libCode, isbn))
            }
    }

    private fun toResult(
        libCode: String,
        isbn: String,
        response: Data4LibraryBookExistResponse
    ): Data4LibraryBookExistResult {
        val result = response.response.result

        return Data4LibraryBookExistResult(
            libCode = libCode,
            isbn = isbn,
            hasBook = when (result.hasBook) {
                "Y" -> true
                "N" -> false
                else -> null
            },
            loanAvailable = when (result.loanAvailable) {
                "Y" -> true
                "N" -> false
                else -> null
            },
            status = when (result.hasBook) {
                "Y" -> LibraryBookAvailabilityStatus.AVAILABLE
                "N" -> LibraryBookAvailabilityStatus.UNAVAILABLE
                else -> LibraryBookAvailabilityStatus.UNKNOWN
            }
        )
    }

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt) / 1_000_000
}

class Data4LibraryBookExistResult(
    val libCode: String,
    val isbn: String,
    val hasBook: Boolean?,
    val loanAvailable: Boolean?,
    val status: LibraryBookAvailabilityStatus
) {
    companion object {
        fun unknown(
            libCode: String,
            isbn: String
        ): Data4LibraryBookExistResult {
            return Data4LibraryBookExistResult(
                libCode = libCode,
                isbn = isbn,
                hasBook = null,
                loanAvailable = null,
                status = LibraryBookAvailabilityStatus.UNKNOWN
            )
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Data4LibraryBookExistResponse(
    val response: BookExistResponseBody = BookExistResponseBody()
)

@JsonIgnoreProperties(ignoreUnknown = true)
class BookExistResponseBody(
    val resultNum: Int? = null,
    val result: BookExistResult = BookExistResult()
)

@JsonIgnoreProperties(ignoreUnknown = true)
class BookExistResult(
    val hasBook: String? = null,
    val loanAvailable: String? = null
)
