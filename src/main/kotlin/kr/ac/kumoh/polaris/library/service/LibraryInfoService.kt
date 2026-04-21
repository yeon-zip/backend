package kr.ac.kumoh.polaris.library.service

import kr.ac.kumoh.polaris.bookmark.implement.BookmarkStatusReader
import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import kr.ac.kumoh.polaris.library.implement.LibraryDetailReader
import kr.ac.kumoh.polaris.library.implement.NearbyLibraryReader
import kr.ac.kumoh.polaris.library.implement.dto.LibraryDetailResult
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryItemResult
import org.springframework.stereotype.Service

@Service
class LibraryInfoService(
    private val libraryDetailReader: LibraryDetailReader,
    private val nearbyLibraryReader: NearbyLibraryReader,
    private val bookmarkStatusReader: BookmarkStatusReader
) {
    /**
     * 도서관 상세 조회
     *
     * @param libraryId 도서관 ID
     */
    fun getLibraryDetail(
        libraryId: Long,
        userId: Long? = null
    ): LibraryDetailResult {
        val result = libraryDetailReader.read(libraryId)

        return result.copy(
            isBookmarked = bookmarkStatusReader.isLibraryBookmarked(userId, result.libraryId)
        )
    }

    /**
     * 주변 도서관 목록 조회
     *
     * @param latitude 위도
     * @param longitude 경도
     * @param radiusKm 반경(km 단위)
     * @param cursor 커서
     * @param limit 조회 개수
     */
    fun getNearbyLibraries(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        cursor: String?,
        limit: Int,
        userId: Long? = null
    ): CursorPageResult<NearbyLibraryItemResult> {
        val result = nearbyLibraryReader.read(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            cursor = cursor,
            limit = limit
        )

        val bookmarkedLibraryIds = bookmarkStatusReader.getBookmarkedLibraryIds(
            userId = userId,
            libraryIds = result.items.map { it.libraryId }
        )

        return result.copy(
            items = result.items.map { item ->
                item.copy(isBookmarked = bookmarkedLibraryIds.contains(item.libraryId))
            }
        )
    }
}
