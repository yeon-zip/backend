package kr.ac.kumoh.polaris.bookavailability.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.ac.kumoh.polaris.bookavailability.implement.dto.BookHoldingItemResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityStatus

data class BookHoldingItemResponse(
    @Schema(description = "도서관 ID입니다. 최대 길이는 19자입니다.", example = "3372036854775808", nullable = false)
    val libraryId: Long,
    @Schema(description = "도서관명입니다.", example = "구미시립양포도서관", nullable = false)
    val name: String,
    @Schema(description = "도서관 주소입니다.", example = "경상북도 구미시 옥계북로 51", nullable = false)
    val address: String,
    @Schema(description = "도서관이 위치한 곳의 위도입니다.", example = "36.1387724", nullable = true)
    val latitude: Double?,
    @Schema(description = "도서관이 위치한 곳의 경도입니다.", example = "128.4187321", nullable = true)
    val longitude: Double?,
    @Schema(description = "요청 위치에서 도서관까지 떨어진 거리입니다.", example = "2.38", nullable = false)
    val distanceKm: Double,
    @Schema(description = "도서 소장 여부입니다.", example = "true", nullable = false)
    val hasBook: Boolean?,
    @Schema(description = "도서 대출 가능 여부입니다.", example = "false", nullable = false)
    val loanAvailable: Boolean?,
    @Schema(description = "도서 이용 상태입니다. 이용 가능(<code>AVAILABLE</code>), 이용 불가(<code>UNAVAILABLE</code>), 알 수 없음(<code>UNKNOWN</code>)의 값을 가집니다. 상태가 새롭게 추가될 수 있습니다.", example = "UNAVAILABLE", nullable = false)
    val availabilityStatus: LibraryBookAvailabilityStatus,
    @Schema(description = "도서관 운영 여부입니다.", example = "true", nullable = false)

    val openNow: Boolean
) {
    companion object {
        fun from(result: BookHoldingItemResult): BookHoldingItemResponse =
            BookHoldingItemResponse(
                libraryId = result.libraryId,
                name = result.name,
                address = result.address,
                latitude = result.latitude,
                longitude = result.longitude,
                distanceKm = result.distanceKm,
                hasBook = result.hasBook,
                loanAvailable = result.loanAvailable,
                availabilityStatus = result.availabilityStatus,
                openNow = result.openNow
            )
    }
}
