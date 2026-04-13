package kr.ac.kumoh.polaris.library.implement

import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryCursor
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryItemResult
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryQueryResult
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NearbyLibraryReader(
    private val nearbyLibraryQueryRepository: NearbyLibraryQueryRepository,
    private val libraryOpenStatusResolver: LibraryOpenStatusResolver
) {
    /**
     * 주변 도서관 목록 조회
     *
     * @param latitude 위도
     * @param longitude 경도
     * @param radiusKm 반경 (km 단위)
     * @param cursor 커서
     * @param limit 조회 개수
     */
    fun read(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        cursor: String?,
        limit: Int
    ): CursorPageResult<NearbyLibraryItemResult> {
        validate(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            limit = limit
        )

        val parsedCursor = cursor?.let(::parseCursor)
        val nearbyLibraries = nearbyLibraryQueryRepository.findPageWithinRadius(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            cursor = parsedCursor,
            limit = limit + 1
        )
        val hasNext = nearbyLibraries.size > limit
        val pageItems = if (hasNext) nearbyLibraries.take(limit) else nearbyLibraries
        val nextCursor = if (hasNext) {
            pageItems.lastOrNull()?.toCursor()
        } else {
            null
        }

        val openNowStatuses = libraryOpenStatusResolver.getOpenNowStatuses(
            libraryIds = pageItems.map { it.libraryId },
            now = LocalDateTime.now()
        ).associate { status ->
            status.libraryId to status.openNow
        }

        return CursorPageResult(
            nextCursor = nextCursor,
            hasNext = hasNext,
            items = pageItems.map { nearbyLibrary ->
                NearbyLibraryItemResult(
                    libraryId = nearbyLibrary.libraryId,
                    name = nearbyLibrary.name,
                    address = nearbyLibrary.address,
                    latitude = nearbyLibrary.latitude,
                    longitude = nearbyLibrary.longitude,
                    homepageUrl = nearbyLibrary.homepageUrl,
                    tel = nearbyLibrary.tel,
                    distanceKm = nearbyLibrary.distanceKm,
                    openNow = openNowStatuses[nearbyLibrary.libraryId] ?: false
                )
            }
        )
    }

    /**
     * 파라미터 검증
     */
    private fun validate(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        limit: Int
    ) {
        if (latitude !in -90.0 .. 90.0) {
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        }
        if (longitude !in -180.0 .. 180.0) {
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        }
        if (radiusKm <= 0) {
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        }
        if (limit !in 1..100) {
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        }
    }

    /**
     * 커서 파싱
     */
    private fun parseCursor(rawCursor: String): NearbyLibraryCursor {
        val parts = rawCursor.split(":")
        if (parts.size != 2) {
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        }

        val distanceMeter = parts[0].toLongOrNull()
            ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        val libraryId = parts[1].toLongOrNull()
            ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)

        return NearbyLibraryCursor(
            distanceMeter = distanceMeter,
            libraryId = libraryId
        )
    }

    /**
     * 커서 생성
     */
    private fun NearbyLibraryQueryResult.toCursor(): String = "$distanceMeter:$libraryId"
}
