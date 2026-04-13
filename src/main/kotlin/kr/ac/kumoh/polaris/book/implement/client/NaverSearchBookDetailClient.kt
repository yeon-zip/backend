package kr.ac.kumoh.polaris.book.implement.client

import kr.ac.kumoh.polaris.book.implement.client.dto.NaverBookDetailSearchResponse
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.global.properties.NaverSearchApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.*

@Component
class NaverSearchBookDetailClient(
    private val naverSearchDetailRestClient: RestClient,
    private val properties: NaverSearchApiProperties
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun searchBookDetails(
        title: String? = null,
        isbn: String? = null,
        display: Int = 10,
        start: Int = 1,
        sort: String = "sim"
    ): NaverBookDetailSearchResponse {
        val normalizedTitle = title?.trim()?.takeIf { it.isNotBlank() }
        val normalizedIsbn = isbn?.normalizeIsbn()?.takeIf { it.isNotBlank() }

        validate(
            title = normalizedTitle,
            isbn = normalizedIsbn,
            display = display,
            start = start,
            sort = sort
        )

        log.debug(
            "네이버 책 상세 검색 결과 조회 요청을 시작합니다: title={}, isbn={}, display={}, start={}, sort={}",
            normalizedTitle,
            normalizedIsbn,
            display,
            start,
            sort
        )

        val response = try {
            naverSearchDetailRestClient.get()
                .uri { builder ->
                    builder.queryParamIfPresent("d_titl", Optional.ofNullable(normalizedTitle))
                        .queryParamIfPresent("d_isbn", Optional.ofNullable(normalizedIsbn))
                        .queryParam("display", display)
                        .queryParam("start", start)
                        .queryParam("sort", sort)
                        .build()
                }
                .header(NaverSearchApiProperties.HEADER_CLIENT_ID, properties.list.clientId)
                .header(NaverSearchApiProperties.HEADER_CLIENT_SECRET, properties.list.clientSecret)
                .retrieve()
                .body<NaverBookDetailSearchResponse>()
                ?: throw ServiceException(
                    errorCode = ErrorCode.EXTERNAL_API_RESPONSE_PARSE_FAILED,
                    message = "네이버 책 상세 검색 응답 본문이 비어 있습니다: title=$normalizedTitle, isbn=$normalizedIsbn"
                )
        } catch (e: ServiceException) {
            log.error("네이버 책 상세 검색 결과 조회 요청에 실패했습니다: title=$normalizedTitle, isbn=$normalizedIsbn")
            throw e
        } catch (e: Exception) {
            log.error(
                "네이버 책 상세 검색 결과 조회 요청에 실패했습니다: title={}, isbn={}, display={}, start={}, sort={}",
                normalizedTitle,
                normalizedIsbn,
                display,
                start,
                sort,
                e
            )
            throw ServiceException(
                errorCode = ErrorCode.EXTERNAL_API_COMMUNICATION_FAILED,
                message = "네이버 책 상세 검색 호출에 실패했습니다: title=$normalizedTitle, isbn=$normalizedIsbn"
            )
        }

        log.debug(
            "네이버 책 상세 검색 결과 조회 요청에 성공했습니다: total={}, start={}, display={}, itemCount={}",
            response.total,
            response.start,
            response.display,
            response.items.size
        )

        return response
    }

    private fun validate(
        title: String?,
        isbn: String?,
        display: Int,
        start: Int,
        sort: String
    ) {
        if (title == null && isbn == null) {
            throw ServiceException(
                errorCode = ErrorCode.INVALID_INPUT_VALUE,
                message = "도서명 또는 ISBN 중 하나는 입력해야 해요."
            )
        }

        if (display !in 1..100) {
            throw ServiceException(
                errorCode = ErrorCode.INVALID_INPUT_VALUE,
                message = "한 번에 표시할 검색 결과 개수는 1 이상 100 이하여야 해요: display=$display"
            )
        }

        if (start !in 1..1000) {
            throw ServiceException(
                errorCode = ErrorCode.INVALID_INPUT_VALUE,
                message = "검색 시작 위치는 1 이상 1000 이하여야 해요: start=$start"
            )
        }

        if (sort !in setOf("sim", "date")) {
            throw ServiceException(
                errorCode = ErrorCode.INVALID_INPUT_VALUE,
                message = "sort는 정확도순(sim) 또는 출간일순(date)이어야 해요: sort=$sort"
            )
        }
    }

    private fun String.normalizeIsbn(): String =
        replace(Regex("[^0-9Xx ]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
