package kr.ac.kumoh.polaris.book.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookSearchReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.book.implement.dto.BookSearchItemResult
import kr.ac.kumoh.polaris.bookmark.implement.BookmarkStatusReader
import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BookBookmarkViewServiceTest {
    @Test
    fun `get book returns bookmarked true for authenticated user`() {
        val bookReader = mock(BookReader::class.java)
        val bookmarkStatusReader = mock(BookmarkStatusReader::class.java)
        val book = persistedBook(id = 1L, isbn = "9791198363510")
        val service = BookService(
            bookReader = bookReader,
            bookMetadataLoader = mock(BookMetadataLoader::class.java),
            bookWriter = mock(BookWriter::class.java),
            bookmarkStatusReader = bookmarkStatusReader
        )

        `when`(bookReader.findByIsbn("9791198363510")).thenReturn(book)
        `when`(bookmarkStatusReader.isBookBookmarked(7L, 1L)).thenReturn(true)

        val result = service.getBook("9791198363510", 7L)

        assertTrue(result.isBookmarked)
    }

    @Test
    fun `get book returns bookmarked false for unauthenticated user`() {
        val bookReader = mock(BookReader::class.java)
        val bookmarkStatusReader = mock(BookmarkStatusReader::class.java)
        val book = persistedBook(id = 1L, isbn = "9791198363510")
        val service = BookService(
            bookReader = bookReader,
            bookMetadataLoader = mock(BookMetadataLoader::class.java),
            bookWriter = mock(BookWriter::class.java),
            bookmarkStatusReader = bookmarkStatusReader
        )

        `when`(bookReader.findByIsbn("9791198363510")).thenReturn(book)

        val result = service.getBook("9791198363510", null)

        assertFalse(result.isBookmarked)
    }

    @Test
    fun `search books enriches bookmarked flags in batch`() {
        val bookSearchReader = mock(BookSearchReader::class.java)
        val bookmarkStatusReader = mock(BookmarkStatusReader::class.java)
        val service = BookSearchService(bookSearchReader, bookmarkStatusReader)
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

        val result = service.searchBooks("테스트", null, 10, 7L)

        assertTrue(result.items.first().isBookmarked)
        assertFalse(result.items.last().isBookmarked)
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
