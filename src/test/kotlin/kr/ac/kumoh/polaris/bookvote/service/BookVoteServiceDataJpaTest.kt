package kr.ac.kumoh.polaris.bookvote.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.implement.BookMetadata
import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.book.repository.BookRepository
import kr.ac.kumoh.polaris.bookvote.entity.BookVoteType
import kr.ac.kumoh.polaris.bookvote.implement.BookVoteSummaryReader
import kr.ac.kumoh.polaris.bookvote.repository.BookVoteRepository
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.implement.UserReader
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@DataJpaTest
@Import(
    BookVoteService::class,
    BookVoteSummaryReader::class,
    UserReader::class,
    BookReader::class,
    BookWriter::class,
    BookVoteServiceDataJpaTest.TestConfig::class
)
class BookVoteServiceDataJpaTest(
    @Autowired private val bookVoteService: BookVoteService,
    @Autowired private val bookVoteSummaryReader: BookVoteSummaryReader,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val bookRepository: BookRepository,
    @Autowired private val bookVoteRepository: BookVoteRepository
) {
    @Test
    fun `user can create recommend vote`() {
        val user = userRepository.save(testUser("recommend-create"))
        val book = bookRepository.save(testBook("9791191111111", "추천 책"))

        bookVoteService.voteBook(user.id!!, book.isbn!!, BookVoteType.RECOMMEND)

        val vote = bookVoteRepository.findByUserIdAndBookId(user.id!!, book.id!!)
        val summary = bookVoteSummaryReader.getBookVoteSummary(user.id!!, book.isbn!!)

        assertEquals(1, bookVoteRepository.count())
        assertEquals(BookVoteType.RECOMMEND, vote?.voteType)
        assertEquals(1L, summary.recommendCount)
        assertEquals(0L, summary.notRecommendCount)
        assertEquals(BookVoteType.RECOMMEND, summary.myVote)
    }

    @Test
    fun `user can create not recommend vote`() {
        val user = userRepository.save(testUser("not-recommend-create"))
        val book = bookRepository.save(testBook("9791192222222", "비추천 책"))

        bookVoteService.voteBook(user.id!!, book.isbn!!, BookVoteType.NOT_RECOMMEND)

        val vote = bookVoteRepository.findByUserIdAndBookId(user.id!!, book.id!!)
        val summary = bookVoteSummaryReader.getBookVoteSummary(user.id!!, book.isbn!!)

        assertEquals(1, bookVoteRepository.count())
        assertEquals(BookVoteType.NOT_RECOMMEND, vote?.voteType)
        assertEquals(0L, summary.recommendCount)
        assertEquals(1L, summary.notRecommendCount)
        assertEquals(BookVoteType.NOT_RECOMMEND, summary.myVote)
    }

    @Test
    fun `same user voting same value again does not create duplicate row`() {
        val user = userRepository.save(testUser("same-value"))
        val book = bookRepository.save(testBook("9791193333333", "같은 값 재투표 책"))

        bookVoteService.voteBook(user.id!!, book.isbn!!, BookVoteType.RECOMMEND)
        bookVoteService.voteBook(user.id!!, book.isbn!!, BookVoteType.RECOMMEND)

        val vote = bookVoteRepository.findByUserIdAndBookId(user.id!!, book.id!!)

        assertEquals(1, bookVoteRepository.count())
        assertEquals(BookVoteType.RECOMMEND, vote?.voteType)
    }

    @Test
    fun `same user can change vote from recommend to not recommend without new row`() {
        val user = userRepository.save(testUser("recommend-to-not"))
        val book = bookRepository.save(testBook("9791194444444", "투표 변경 책1"))

        bookVoteService.voteBook(user.id!!, book.isbn!!, BookVoteType.RECOMMEND)
        bookVoteService.voteBook(user.id!!, book.isbn!!, BookVoteType.NOT_RECOMMEND)

        val vote = bookVoteRepository.findByUserIdAndBookId(user.id!!, book.id!!)
        val summary = bookVoteSummaryReader.getBookVoteSummary(user.id!!, book.isbn!!)

        assertEquals(1, bookVoteRepository.count())
        assertEquals(BookVoteType.NOT_RECOMMEND, vote?.voteType)
        assertEquals(0L, summary.recommendCount)
        assertEquals(1L, summary.notRecommendCount)
        assertEquals(BookVoteType.NOT_RECOMMEND, summary.myVote)
    }

    @Test
    fun `same user can change vote from not recommend to recommend without new row`() {
        val user = userRepository.save(testUser("not-to-recommend"))
        val book = bookRepository.save(testBook("9791195555555", "투표 변경 책2"))

        bookVoteService.voteBook(user.id!!, book.isbn!!, BookVoteType.NOT_RECOMMEND)
        bookVoteService.voteBook(user.id!!, book.isbn!!, BookVoteType.RECOMMEND)

        val vote = bookVoteRepository.findByUserIdAndBookId(user.id!!, book.id!!)
        val summary = bookVoteSummaryReader.getBookVoteSummary(user.id!!, book.isbn!!)

        assertEquals(1, bookVoteRepository.count())
        assertEquals(BookVoteType.RECOMMEND, vote?.voteType)
        assertEquals(1L, summary.recommendCount)
        assertEquals(0L, summary.notRecommendCount)
        assertEquals(BookVoteType.RECOMMEND, summary.myVote)
    }

    @Test
    fun `vote summary counts are accurate across multiple users`() {
        val firstUser = userRepository.save(testUser("multi-1"))
        val secondUser = userRepository.save(testUser("multi-2"))
        val thirdUser = userRepository.save(testUser("multi-3"))
        val book = bookRepository.save(testBook("9791196666666", "집계 책"))

        bookVoteService.voteBook(firstUser.id!!, book.isbn!!, BookVoteType.RECOMMEND)
        bookVoteService.voteBook(secondUser.id!!, book.isbn!!, BookVoteType.RECOMMEND)
        bookVoteService.voteBook(thirdUser.id!!, book.isbn!!, BookVoteType.NOT_RECOMMEND)

        val summaryForFirstUser = bookVoteSummaryReader.getBookVoteSummary(firstUser.id!!, book.isbn!!)
        val summaryForAnonymous = bookVoteSummaryReader.getBookVoteSummary(null, book.isbn!!)

        assertEquals(2L, summaryForFirstUser.recommendCount)
        assertEquals(1L, summaryForFirstUser.notRecommendCount)
        assertEquals(BookVoteType.RECOMMEND, summaryForFirstUser.myVote)
        assertEquals(2L, summaryForAnonymous.recommendCount)
        assertEquals(1L, summaryForAnonymous.notRecommendCount)
        assertNull(summaryForAnonymous.myVote)
    }

    @Test
    fun `voting unknown book throws book not found`() {
        val user = userRepository.save(testUser("missing-book"))

        val exception = assertThrows<ServiceException> {
            bookVoteService.voteBook(user.id!!, "9791999999999", BookVoteType.RECOMMEND)
        }

        assertEquals(ErrorCode.BOOK_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `voting with unknown user throws user not found`() {
        val book = bookRepository.save(testBook("9791197777777", "사용자 없음 책"))

        val exception = assertThrows<ServiceException> {
            bookVoteService.voteBook(999999L, book.isbn!!, BookVoteType.RECOMMEND)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `vote bootstraps missing book with normalized isbn13 from metadata loader`() {
        val user = userRepository.save(testUser("bootstrap-book"))

        bookVoteService.voteBook(user.id!!, "9788936434120", BookVoteType.RECOMMEND)

        val savedBook = bookRepository.findByIsbn("9788936434120")
        val summary = bookVoteSummaryReader.getBookVoteSummary(user.id!!, "9788936434120")

        assertEquals("9788936434120", savedBook?.isbn)
        assertEquals(1L, summary.recommendCount)
        assertEquals(0L, summary.notRecommendCount)
        assertEquals(BookVoteType.RECOMMEND, summary.myVote)
    }

    private fun testUser(suffix: String): User = User(
        provider = UserAuthProvider.KAKAO,
        oidcIssuer = "https://kauth.kakao.com",
        oidcSubject = "subject-$suffix",
        nickname = "tester-$suffix",
        email = "tester-$suffix@example.com"
    )

    private fun testBook(isbn: String, title: String): Book = Book(
        isbn = isbn,
        title = title,
        author = "테스트 저자",
        coverImageUrl = "https://example.com/$isbn.jpg"
    )

    @TestConfiguration
    class TestConfig {
        @Bean
        fun bookMetadataLoader(): BookMetadataLoader =
            mock(BookMetadataLoader::class.java).apply {
                `when`(loadByIsbn("9788936434120")).thenReturn(
                    BookMetadata(
                        isbn = "9788936434120",
                        title = "아몬드",
                        author = "손원평",
                        publisher = "창비",
                        description = "테스트 설명",
                        publicationDate = null,
                        coverImageUrl = "https://example.com/9788936434120.jpg"
                    )
                )
            }
    }
}
