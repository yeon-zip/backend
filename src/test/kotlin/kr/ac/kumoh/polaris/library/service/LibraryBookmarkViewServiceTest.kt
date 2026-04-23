package kr.ac.kumoh.polaris.library.service

import kr.ac.kumoh.polaris.bookmark.implement.BookmarkStatusReader
import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import kr.ac.kumoh.polaris.library.implement.LibraryDetailReader
import kr.ac.kumoh.polaris.library.implement.NearbyLibraryReader
import kr.ac.kumoh.polaris.library.implement.dto.LibraryDetailResult
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryItemResult
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class LibraryBookmarkViewServiceTest {
    @Test
    fun `get library detail returns bookmarked true for authenticated user`() {
        val libraryDetailReader = mock(LibraryDetailReader::class.java)
        val bookmarkStatusReader = mock(BookmarkStatusReader::class.java)
        val service = LibraryInfoService(
            libraryDetailReader = libraryDetailReader,
            nearbyLibraryReader = mock(NearbyLibraryReader::class.java),
            bookmarkStatusReader = bookmarkStatusReader
        )
        val detailResult = LibraryDetailResult(
            libraryId = 3L,
            name = "시립도서관",
            address = "경북 구미시",
            latitude = 36.0,
            longitude = 128.0,
            homepageUrl = null,
            tel = null,
            openNow = true,
            isBookmarked = false,
            todayOperatingHour = null,
            weeklyOperatingHours = emptyList(),
            closedRules = emptyList()
        )

        `when`(libraryDetailReader.read(3L)).thenReturn(detailResult)
        `when`(bookmarkStatusReader.isLibraryBookmarked(7L, 3L)).thenReturn(true)

        val result = service.getLibraryDetail(3L, 7L)

        assertTrue(result.isBookmarked)
    }

    @Test
    fun `nearby libraries enrich bookmarked flags in batch`() {
        val nearbyLibraryReader = mock(NearbyLibraryReader::class.java)
        val bookmarkStatusReader = mock(BookmarkStatusReader::class.java)
        val service = LibraryInfoService(
            libraryDetailReader = mock(LibraryDetailReader::class.java),
            nearbyLibraryReader = nearbyLibraryReader,
            bookmarkStatusReader = bookmarkStatusReader
        )
        val nearbyResult = CursorPageResult(
            hasNext = false,
            nextCursor = null,
            items = listOf(
                NearbyLibraryItemResult(
                    libraryId = 1L,
                    name = "도서관1",
                    address = "주소1",
                    latitude = 36.0,
                    longitude = 128.0,
                    homepageUrl = null,
                    tel = null,
                    distanceKm = 1.2,
                    openNow = true,
                    isBookmarked = false
                ),
                NearbyLibraryItemResult(
                    libraryId = 2L,
                    name = "도서관2",
                    address = "주소2",
                    latitude = 36.0,
                    longitude = 128.0,
                    homepageUrl = null,
                    tel = null,
                    distanceKm = 2.1,
                    openNow = false,
                    isBookmarked = false
                )
            )
        )

        `when`(nearbyLibraryReader.read(36.0, 128.0, 5.0, null, 10)).thenReturn(nearbyResult)
        `when`(
            bookmarkStatusReader.getBookmarkedLibraryIds(
                7L,
                listOf(1L, 2L)
            )
        ).thenReturn(setOf(2L))

        val result = service.getNearbyLibraries(36.0, 128.0, 5.0, null, 10, 7L)

        assertFalse(result.items.first().isBookmarked)
        assertTrue(result.items.last().isBookmarked)
    }
}
