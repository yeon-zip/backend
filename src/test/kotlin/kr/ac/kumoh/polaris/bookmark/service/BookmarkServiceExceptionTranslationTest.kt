package kr.ac.kumoh.polaris.bookmark.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.bookmark.entity.BookBookmark
import kr.ac.kumoh.polaris.bookmark.entity.LibraryBookmark
import kr.ac.kumoh.polaris.bookmark.repository.BookBookmarkRepository
import kr.ac.kumoh.polaris.bookmark.repository.LibraryBookmarkRepository
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.library.entity.Library
import kr.ac.kumoh.polaris.library.implement.LibraryReader
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.implement.UserReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.dao.DataIntegrityViolationException

class BookmarkServiceExceptionTranslationTest {
    @Test
    fun `library bookmark unique constraint violation becomes bookmark already exists`() {
        val userReader = mock(UserReader::class.java)
        val libraryReader = mock(LibraryReader::class.java)
        val bookReader = mock(BookReader::class.java)
        val bookMetadataLoader = mock(BookMetadataLoader::class.java)
        val bookWriter = mock(BookWriter::class.java)
        val libraryBookmarkRepository = mock(LibraryBookmarkRepository::class.java)
        val bookBookmarkRepository = mock(BookBookmarkRepository::class.java)
        val service = BookmarkService(
            userReader = userReader,
            libraryReader = libraryReader,
            bookReader = bookReader,
            bookMetadataLoader = bookMetadataLoader,
            bookWriter = bookWriter,
            libraryBookmarkRepository = libraryBookmarkRepository,
            bookBookmarkRepository = bookBookmarkRepository
        )
        val user = testUser(7L)
        val library = testLibrary(3L)

        `when`(userReader.findByIdOrThrow(7L)).thenReturn(user)
        `when`(libraryReader.findByIdOrThrow(3L)).thenReturn(library)
        `when`(libraryBookmarkRepository.existsByUserIdAndLibraryId(7L, 3L)).thenReturn(false)
        `when`(libraryBookmarkRepository.saveAndFlush(any(LibraryBookmark::class.java)))
            .thenThrow(DataIntegrityViolationException("duplicate key"))

        val exception = assertThrows<ServiceException> {
            service.bookmarkLibrary(7L, 3L)
        }

        assertEquals(ErrorCode.BOOKMARK_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `book bookmark unique constraint violation becomes bookmark already exists`() {
        val userReader = mock(UserReader::class.java)
        val libraryReader = mock(LibraryReader::class.java)
        val bookReader = mock(BookReader::class.java)
        val bookMetadataLoader = mock(BookMetadataLoader::class.java)
        val bookWriter = mock(BookWriter::class.java)
        val libraryBookmarkRepository = mock(LibraryBookmarkRepository::class.java)
        val bookBookmarkRepository = mock(BookBookmarkRepository::class.java)
        val service = BookmarkService(
            userReader = userReader,
            libraryReader = libraryReader,
            bookReader = bookReader,
            bookMetadataLoader = bookMetadataLoader,
            bookWriter = bookWriter,
            libraryBookmarkRepository = libraryBookmarkRepository,
            bookBookmarkRepository = bookBookmarkRepository
        )
        val user = testUser(7L)
        val book = testBook(11L, "9791198363510")

        `when`(userReader.findByIdOrThrow(7L)).thenReturn(user)
        `when`(bookReader.findByIsbn("9791198363510")).thenReturn(book)
        `when`(bookBookmarkRepository.existsByUserIdAndBookId(7L, 11L)).thenReturn(false)
        `when`(bookBookmarkRepository.saveAndFlush(any(BookBookmark::class.java)))
            .thenThrow(DataIntegrityViolationException("duplicate key"))

        val exception = assertThrows<ServiceException> {
            service.bookmarkBook(7L, "9791198363510")
        }

        assertEquals(ErrorCode.BOOKMARK_ALREADY_EXISTS, exception.errorCode)
    }

    private fun testUser(id: Long): User {
        val user = User(
            provider = UserAuthProvider.KAKAO,
            oidcIssuer = "https://kauth.kakao.com",
            oidcSubject = "subject-$id",
            nickname = "tester$id",
            email = "tester$id@example.com"
        )
        val field = User::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(user, id)
        return user
    }

    private fun testLibrary(id: Long): Library {
        val library = Library(
            name = "테스트 도서관",
            libCode = "lib-$id"
        )
        val field = Library::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(library, id)
        return library
    }

    private fun testBook(id: Long, isbn: String): Book {
        val book = Book(
            isbn = isbn,
            title = "테스트 책"
        )
        val field = Book::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(book, id)
        return book
    }
}
