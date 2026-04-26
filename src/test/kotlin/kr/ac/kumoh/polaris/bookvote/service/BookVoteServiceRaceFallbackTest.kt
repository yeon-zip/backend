package kr.ac.kumoh.polaris.bookvote.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.bookvote.entity.BookVote
import kr.ac.kumoh.polaris.bookvote.entity.BookVoteType
import kr.ac.kumoh.polaris.bookvote.repository.BookVoteRepository
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.implement.UserReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.dao.DataIntegrityViolationException

class BookVoteServiceRaceFallbackTest {
    @Test
    fun `unique constraint race falls back to reload and update existing vote`() {
        val userReader = mock(UserReader::class.java)
        val bookReader = mock(BookReader::class.java)
        val bookMetadataLoader = mock(BookMetadataLoader::class.java)
        val bookWriter = mock(BookWriter::class.java)
        val bookVoteRepository = mock(BookVoteRepository::class.java)
        val service = BookVoteService(
            userReader = userReader,
            bookReader = bookReader,
            bookMetadataLoader = bookMetadataLoader,
            bookWriter = bookWriter,
            bookVoteRepository = bookVoteRepository
        )
        val user = testUser(7L)
        val book = testBook(11L, "9791198363510")
        val existingVote = BookVote(user = user, book = book, voteType = BookVoteType.RECOMMEND)

        `when`(userReader.findByIdOrThrow(7L)).thenReturn(user)
        `when`(bookReader.findByIsbn("9791198363510")).thenReturn(book)
        `when`(bookVoteRepository.findByUserIdAndBookId(7L, 11L)).thenReturn(null, existingVote)
        `when`(bookVoteRepository.saveAndFlush(any(BookVote::class.java)))
            .thenThrow(DataIntegrityViolationException("duplicate key"))

        service.voteBook(7L, "9791198363510", BookVoteType.NOT_RECOMMEND)

        assertEquals(BookVoteType.NOT_RECOMMEND, existingVote.voteType)
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
