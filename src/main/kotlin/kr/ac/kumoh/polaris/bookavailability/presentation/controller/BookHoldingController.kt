package kr.ac.kumoh.polaris.bookavailability.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.ac.kumoh.polaris.bookavailability.presentation.response.BookHoldingItemResponse
import kr.ac.kumoh.polaris.bookavailability.service.BookAvailabilityService
import kr.ac.kumoh.polaris.global.dto.CursorPageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "도서 가능 여부")
@RestController
@RequestMapping("/api/v1/book-availability")
class BookHoldingController(
    private val bookAvailabilityService: BookAvailabilityService
) {
    @Operation(
        summary = "주변 도서관의 도서 소장 정보 조회",
        description = """
            <p>쿼리 파리미터 <code>latitude</code>와 <code>longitude</code>에 해당하는 위도와 경도를 기준으로 쿼리 파라미터 <code>radiusKm</code> 반경에 위치한 도서관의 목록을 검색한 후 쿼리 파라미터 <code>isbn</code>에 해당하는 도서를 각 도서관이 소장하고 있는지 조회합니다.</p>
            <p>응답의 <code>nextCursor</code> 값을 요청 시 쿼리 파라미터 <code>cursor</code>로 지정하면 해당 아이템 이후의 목록을 조회합니다.</p>
            <p>* 주변 도서관의 도서 소장 정보 조회는 최대 60초 소요됩니다. 시간 초과 한도를 60초로 설정하세요. 비정상적으로 많은 요청을 보내면 HTTP 상태 코드 <code>429 Too Many Requests</code>가 응답될 수 있습니다.</p>
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공"),
        ]
    )
    @Parameters(
        value = [
            Parameter(name = "isbn", description = "도서의 ISBN입니다. 13자리 값이어야 합니다."),
            Parameter(name = "latitude", description = "조회하려는 위치의 위도입니다. -90.0 ~ +90.0 사이의 값이어야 합니다."),
            Parameter(name = "longitude", description = "조회하려는 위치의 경도입니다. -180.0 ~ +180.0 사이의 값이어야 합니다."),
            Parameter(name = "radiusKm", description = "조회하려는 위치에서 탐색할 범위입니다. 이는 원의 반지름이며, 조회하려는 위치가 원의 중심이 됩니다."),
            Parameter(name = "loanAvailable", description = "요청 시각에 대출 가능한 도서관만 출력할지 결정합니다. <code>false</code>이면 대출 가능 여부에 상관 없이 조회합니다."),
            Parameter(name = "openNow", description = "요청 시각에 운영 중인 도서관만 출력할지 결정합니다. <code>false</code>이면 운영 여부에 상관 없이 조회합니다."),
            Parameter(name = "cursor", description = "특정 도서관 아이템 이후의 도서관 아이템을 조회할 때 사용합니다."),
            Parameter(name = "limit", description = "한 번에 응답 받을 도서관의 개수이고 기본값은 10이고 최대값은 100입니다.")
        ]
    )
    @GetMapping
    fun getBookHoldings(
        @RequestParam isbn: String,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(defaultValue = "5.0") radiusKm: Double,
        @RequestParam(required = false) loanAvailable: Boolean?,
        @RequestParam(required = false) openNow: Boolean?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<CursorPageResponse<BookHoldingItemResponse>> {
        val result = bookAvailabilityService.getBookHoldings(
            isbn = isbn,
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            loanAvailable = loanAvailable,
            openNow = openNow,
            cursor = cursor,
            limit = limit
        )

        return ResponseEntity.ok(
            CursorPageResponse.from(result, BookHoldingItemResponse::from)
        )
    }
}
