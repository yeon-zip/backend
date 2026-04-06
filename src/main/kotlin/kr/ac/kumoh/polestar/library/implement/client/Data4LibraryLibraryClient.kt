package kr.ac.kumoh.polestar.library.implement.client

import kr.ac.kumoh.polestar.global.properties.Data4LibraryApiProperties
import kr.ac.kumoh.polestar.library.implement.client.dto.Data4LibraryLibrary
import kr.ac.kumoh.polestar.library.implement.client.dto.Data4LibraryLibraryPageResult
import kr.ac.kumoh.polestar.library.implement.client.dto.Data4LibraryLibrarySearchResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class Data4LibraryLibraryClient(
    private val data4LibraryRestClient: RestClient,
    private val properties: Data4LibraryApiProperties
) {
    private val log = LoggerFactory.getLogger(this.javaClass.simpleName)

    fun searchLibraries(
        pageNo: Int,
        pageSize: Int
    ): Data4LibraryLibraryPageResult {
        log.info("도서관 정보 조회 요청 시작 - pageNo={}, pageSize={}", pageNo, pageSize)

        val responseBody = data4LibraryRestClient.get()
            .uri { builder ->
                builder.path("/api/libSrch")
                    .queryParam("authKey", properties.authKey)
                    .queryParam("pageNo", pageNo)
                    .queryParam("pageSize", pageSize)
                    .queryParam("format", "json")
                    .build()
            }
            .retrieve()
            .body(String::class.java)

        if (responseBody == null) {
            log.warn("도서관 정보 조회 응답이 비어 있습니다 - pageNo={}, pageSize={}", pageNo, pageSize)
            return Data4LibraryLibraryPageResult.empty(pageNo)
        }

        log.info(
            "도서관 정보 원본 응답 - pageNo={}, pageSize={}, body={}",
            pageNo,
            pageSize,
            responseBody.take(1500)
        )

        val response = try {
            jacksonObjectMapper().readValue(responseBody, Data4LibraryLibrarySearchResponse::class.java)
        } catch (e: Exception) {
            log.error(
                "도서관 정보 JSON 파싱 실패 - pageNo={}, pageSize={}, body={}",
                pageNo,
                pageSize,
                responseBody.take(1500),
                e
            )
            return Data4LibraryLibraryPageResult.empty(pageNo)
        }

        val body = response.response
        log.info(
            "도서관 정보 응답 메타 - pageNo={}, pageSize={}, numFound={}, resultNum={}",
            body.pageNo,
            body.pageSize,
            body.numFound,
            body.resultNum
        )

        val libraries = body.libs.mapNotNull { wrapper ->
            val lib = wrapper.lib

            if (lib.libCode.isBlank() || lib.libName.isBlank()) {
                log.debug("도서관 데이터 스킵 - libCode='{}', libName='{}'", lib.libCode, lib.libName)
                return@mapNotNull null
            }

            Data4LibraryLibrary(
                libCode = lib.libCode,
                libName = lib.libName,
                address = lib.address,
                region = null,
                detailRegion = null,
                latitude = lib.latitude?.toDoubleOrNull(),
                longitude = lib.longitude?.toDoubleOrNull(),
                homepage = lib.homepage,
                tel = lib.tel,
                fax = lib.fax,
                operatingTime = lib.operatingTime,
                closed = lib.closed
            )
        }

        log.info(
            "도서관 정보 변환 완료 - pageNo={}, pageSize={}, parsedLibraries={}",
            pageNo,
            pageSize,
            libraries.size
        )

        if (libraries.isNotEmpty()) {
            val first = libraries.first()
            log.info(
                "첫 번째 도서관 샘플 - libCode={}, libName={}, latitude={}, longitude={}",
                first.libCode,
                first.libName,
                first.latitude,
                first.longitude
            )
        }

        return Data4LibraryLibraryPageResult(
            pageNo = body.pageNo,
            pageSize = body.pageSize,
            totalCount = body.numFound,
            resultCount = body.resultNum,
            libraries = libraries
        )
    }
}
