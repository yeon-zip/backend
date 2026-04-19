package kr.ac.kumoh.polaris.bookavailability.implement.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityStatus
import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class Data4LibraryBookExistClient(
    private val data4LibraryWebClient: WebClient,
    private val properties: Data4LibraryApiProperties
) {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val objectMapper = jacksonObjectMapper()

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
            .exchangeToMono { response ->
                val statusCode = response.statusCode()

                response.bodyToMono(String::class.java)
                    .defaultIfEmpty("")
                    .map { responseBody ->
                        RawBookExistResponse(
                            statusCode = statusCode.value(),
                            isSuccessful = statusCode.is2xxSuccessful,
                            body = responseBody
                        )
                    }
            }
            .map { response ->
                mapResponseBody(
                    libCode = libCode,
                    isbn = isbn,
                    response = response,
                    startedAt = startedAt
                )
            }
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

    private fun mapResponseBody(
        libCode: String,
        isbn: String,
        response: RawBookExistResponse,
        startedAt: Long
    ): Data4LibraryBookExistResult {
        val responseBody = response.body.trim()

        if (responseBody.isBlank()) {
            log.warn("도서 소장 여부 응답이 비어 있습니다 - libCode={}, isbn={}", libCode, isbn)
            return Data4LibraryBookExistResult.unknown(libCode, isbn)
        }

        if (!response.isSuccessful) {
            log.error(
                "도서 소장 여부 조회 HTTP 오류 - libCode={}, isbn={}, statusCode={}, body={}, elapsedMs={}",
                libCode,
                isbn,
                response.statusCode,
                abbreviatedBody(responseBody),
                elapsedMillis(startedAt)
            )
            return Data4LibraryBookExistResult.unknown(libCode, isbn)
        }

        extractApiErrorMessage(responseBody)?.let { errorMessage ->
            log.error(
                "도서 소장 여부 조회 API 오류 응답 - libCode={}, isbn={}, error={}, elapsedMs={}",
                libCode,
                isbn,
                errorMessage,
                elapsedMillis(startedAt)
            )
            return Data4LibraryBookExistResult.unknown(libCode, isbn)
        }

        val parsedResponse = try {
            objectMapper.readValue(responseBody, Data4LibraryBookExistResponse::class.java)
        } catch (exception: Exception) {
            log.error(
                "도서 소장 여부 응답 파싱 실패 - libCode={}, isbn={}, body={}, elapsedMs={}",
                libCode,
                isbn,
                abbreviatedBody(responseBody),
                elapsedMillis(startedAt),
                exception
            )
            return Data4LibraryBookExistResult.unknown(libCode, isbn)
        }

        if (parsedResponse.isUnknownPayload()) {
            log.error(
                "도서 소장 여부 조회 비정상 응답 - libCode={}, isbn={}, body={}, elapsedMs={}",
                libCode,
                isbn,
                abbreviatedBody(responseBody),
                elapsedMillis(startedAt)
            )
            return Data4LibraryBookExistResult.unknown(libCode, isbn)
        }

        return toResult(
            libCode = libCode,
            isbn = isbn,
            response = parsedResponse
        )
    }

    private fun extractApiErrorMessage(responseBody: String): String? =
        extractXmlApiErrorMessage(responseBody)
            ?: extractJsonApiErrorMessage(responseBody)

    private fun extractXmlApiErrorMessage(responseBody: String): String? {
        val match = ERROR_RESPONSE_PATTERN.find(responseBody) ?: return null
        return match.groupValues[1]
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private fun extractJsonApiErrorMessage(responseBody: String): String? {
        val match = JSON_ERROR_RESPONSE_PATTERN.find(responseBody)
            ?: JSON_MESSAGE_RESPONSE_PATTERN.find(responseBody)
            ?: return null

        return match.groupValues[1]
            .replace("\\\"", "\"")
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private fun abbreviatedBody(responseBody: String): String =
        responseBody
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_LOGGED_RESPONSE_BODY_LENGTH)

    private fun toResult(
        libCode: String,
        isbn: String,
        response: Data4LibraryBookExistResponse
    ): Data4LibraryBookExistResult {
        val result = response.response.result
        val hasBook = when (result.hasBook) {
            "Y" -> true
            "N" -> false
            else -> null
        }
        val loanAvailable = when (result.loanAvailable) {
            "Y" -> true
            "N" -> false
            else -> null
        }

        return Data4LibraryBookExistResult(
            libCode = libCode,
            isbn = isbn,
            hasBook = hasBook,
            loanAvailable = loanAvailable,
            status = LibraryBookAvailabilityStatus.resolve(hasBook, loanAvailable)
        )
    }

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt) / 1_000_000

    private data class RawBookExistResponse(
        val statusCode: Int,
        val isSuccessful: Boolean,
        val body: String
    )

    companion object {
        private const val MAX_LOGGED_RESPONSE_BODY_LENGTH = 1_500
        private val ERROR_RESPONSE_PATTERN = Regex(
            pattern = "<error>(.*?)</error>",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        private val JSON_ERROR_RESPONSE_PATTERN = Regex(
            pattern = """"error"\s*:\s*"((?:\\.|[^"])*)"""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        private val JSON_MESSAGE_RESPONSE_PATTERN = Regex(
            pattern = """"message"\s*:\s*"((?:\\.|[^"])*)"""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
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
) {
    fun isUnknownPayload(): Boolean =
        response.resultNum == null && response.result.hasBook == null && response.result.loanAvailable == null
}

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
