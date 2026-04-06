package kr.ac.kumoh.polestar.book.implement.client

import kr.ac.kumoh.polestar.book.implement.client.dto.NaverBookSearchResponse
import kr.ac.kumoh.polestar.global.exception.ErrorCode
import kr.ac.kumoh.polestar.global.exception.ServiceException
import kr.ac.kumoh.polestar.global.properties.NaverSearchApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class NaverSearchBookClient(
    private val naverSearchRestClient: RestClient,
    private val properties: NaverSearchApiProperties
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun searchBooks(
        query: String,
        display: Int = 10,
        start: Int = 1,
        sort: String = "sim"
    ): NaverBookSearchResponse {
        log.info(
            "네이버 책 검색 요청 시작 - query={}, display={}, start={}, sort={}",
            query,
            display,
            start,
            sort
        )

        val response = try {
            naverSearchRestClient.get()
                .uri { builder ->
                    builder.path("/v1/search/book.json")
                        .queryParam("query", query)
                        .queryParam("display", display)
                        .queryParam("start", start)
                        .queryParam("sort", sort)
                        .build()
                }
                .header("X-Naver-Client-Id", properties.clientId)
                .header("X-Naver-Client-Secret", properties.clientSecret)
                .retrieve()
                .body(NaverBookSearchResponse::class.java)
                ?: throw ServiceException(
                    errorCode = ErrorCode.NAVER_SEARCH_FAILED,
                    message = "네이버 책 검색 응답 본문이 비어 있습니다. query=$query, start=$start, display=$display, sort=$sort"
                )
        } catch (e: ServiceException) {
            throw e
        } catch (e: Exception) {
            log.error(
                "네이버 책 검색 실패 - query={}, display={}, start={}, sort={}",
                query,
                display,
                start,
                sort,
                e
            )
            throw ServiceException(
                errorCode = ErrorCode.NAVER_SEARCH_FAILED,
                message = "네이버 책 검색 호출에 실패했습니다. query=$query, start=$start, display=$display, sort=$sort"
            )
        }

        log.info(
            "네이버 책 검색 완료 - total={}, start={}, display={}, itemCount={}",
            response.total,
            response.start,
            response.display,
            response.items.size
        )

        return response
    }
}
