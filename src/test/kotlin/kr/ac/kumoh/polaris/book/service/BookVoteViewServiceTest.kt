package kr.ac.kumoh.polaris.book.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookSearchReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.book.implement.dto.BookSearchItemResult
import kr.ac.kumoh.polaris.bookmark.implement.BookmarkStatusReader
import kr.ac.kumoh.polaris.bookvote.entity.BookVoteType
import kr.ac.kumoh.polaris.bookvote.implement.BookVoteSummaryReader
import kr.ac.kumoh.polaris.bookvote.implement.dto.BookVoteSummaryResult
import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class BookVoteViewServiceTest {
    @Test
    fun `get book returns vote summary for authenticated user`() {
        val bookReader = mock(BookReader::class.java)
        val bookmarkStatusReader = mock(BookmarkStatusReader::class.java)
        val bookVoteSummaryReader = mock(BookVoteSummaryReader::class.java)
        val book = persistedBook(id = 1L, isbn = "9791198363510")
        val service = BookService(
            bookReader = bookReader,
            bookMetadataLoader = mock(BookMetadataLoader::class.java),
            bookWriter = mock(BookWriter::class.java),
            bookmarkStatusReader = bookmarkStatusReader,
            bookVoteSummaryReader = bookVoteSummaryReader
        )

        `when`(bookReader.findByIsbn("9791198363510")).thenReturn(book)
        `when`(bookmarkStatusReader.isBookBookmarked(7L, 1L)).thenReturn(true)
        `when`(bookVoteSummaryReader.getBookVoteSummary(7L, "9791198363510"))
            .thenReturn(
                BookVoteSummaryResult(
                    recommendCount = 3,
                    notRecommendCount = 1,
                    myVote = BookVoteType.RECOMMEND
                )
            )

        val result = service.getBook("9791198363510", 7L)

        assertTrue(result.isBookmarked)
        assertEquals(3L, result.recommendCount)
        assertEquals(1L, result.notRecommendCount)
        assertEquals(BookVoteType.RECOMMEND, result.myVote)
    }

    @Test
    fun `get book returns null myVote for unauthenticated user while exposing counts`() {
        val bookReader = mock(BookReader::class.java)
        val bookmarkStatusReader = mock(BookmarkStatusReader::class.java)
        val bookVoteSummaryReader = mock(BookVoteSummaryReader::class.java)
        val book = persistedBook(id = 1L, isbn = "9791198363510")
        val service = BookService(
            bookReader = bookReader,
            bookMetadataLoader = mock(BookMetadataLoader::class.java),
            bookWriter = mock(BookWriter::class.java),
            bookmarkStatusReader = bookmarkStatusReader,
            bookVoteSummaryReader = bookVoteSummaryReader
        )

        `when`(bookReader.findByIsbn("9791198363510")).thenReturn(book)
        `when`(bookVoteSummaryReader.getBookVoteSummary(null, "9791198363510"))
            .thenReturn(
                BookVoteSummaryResult(
                    recommendCount = 2,
                    notRecommendCount = 4,
                    myVote = null
                )
            )

        val result = service.getBook("9791198363510", null)

        assertFalse(result.isBookmarked)
        assertEquals(2L, result.recommendCount)
        assertEquals(4L, result.notRecommendCount)
        assertNull(result.myVote)
    }

    @Test
    fun `search books enriches vote summaries in batch`() {
        val bookSearchReader = mock(BookSearchReader::class.java)
        val bookmarkStatusReader = mock(BookmarkStatusReader::class.java)
        val bookVoteSummaryReader = mock(BookVoteSummaryReader::class.java)
        val service = BookSearchService(bookSearchReader, bookmarkStatusReader, bookVoteSummaryReader)
        val searchResult = CursorPageResult(
            hasNext = false,
            nextCursor = null,
            items = listOf(
                BookSearchItemResult(
                    isbn = "9791198363510",
                    title = "책1",
                    author = "저자1",
                    publisher = null,
                    description = null,
                    publicationDate = null,
                    coverImageUrl = null,
                    link = null,
                    isBookmarked = false
                ),
                BookSearchItemResult(
                    isbn = "9781234567890",
                    title = "책2",
                    author = "저자2",
                    publisher = null,
                    description = null,
                    publicationDate = null,
                    coverImageUrl = null,
                    link = null,
                    isBookmarked = false
                )
            )
        )

        `when`(bookSearchReader.searchBooks("테스트", null, 10)).thenReturn(searchResult)
        `when`(
            bookmarkStatusReader.getBookmarkedIsbns(
                7L,
                listOf("9791198363510", "9781234567890")
            )
        ).thenReturn(setOf("9791198363510"))
        `when`(
            bookVoteSummaryReader.getBookVoteSummaries(
                7L,
                listOf("9791198363510", "9781234567890")
            )
        ).thenReturn(
            mapOf(
                "9791198363510" to BookVoteSummaryResult(
                    recommendCount = 5,
                    notRecommendCount = 1,
                    myVote = BookVoteType.RECOMMEND
                ),
                "9781234567890" to BookVoteSummaryResult(
                    recommendCount = 0,
                    notRecommendCount = 2,
                    myVote = BookVoteType.NOT_RECOMMEND
                )
            )
        )

        val result = service.searchBooks("테스트", null, 10, 7L)

        assertTrue(result.items.first().isBookmarked)
        assertEquals(5L, result.items.first().recommendCount)
        assertEquals(1L, result.items.first().notRecommendCount)
        assertEquals(BookVoteType.RECOMMEND, result.items.first().myVote)
        assertFalse(result.items.last().isBookmarked)
        assertEquals(0L, result.items.last().recommendCount)
        assertEquals(2L, result.items.last().notRecommendCount)
        assertEquals(BookVoteType.NOT_RECOMMEND, result.items.last().myVote)
        verify(bookVoteSummaryReader, times(1)).getBookVoteSummaries(
            7L,
            listOf("9791198363510", "9781234567890")
        )
    }

    private fun persistedBook(
        id: Long,
        isbn: String
    ): Book {
        val book = Book(
            isbn = isbn,
            title = "테스트 책",
            author = "테스트 저자"
        )
        val idField = Book::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(book, id)
        return book
    }
}
