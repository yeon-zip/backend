package kr.ac.kumoh.polaris.bookmark.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.book.implement.client.NaverSearchBookDetailClient
import kr.ac.kumoh.polaris.book.repository.BookRepository
import kr.ac.kumoh.polaris.bookmark.repository.BookBookmarkRepository
import kr.ac.kumoh.polaris.bookmark.repository.LibraryBookmarkRepository
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.global.properties.NaverSearchApiDetailProperties
import kr.ac.kumoh.polaris.global.properties.NaverSearchApiProperties
import kr.ac.kumoh.polaris.library.entity.Address
import kr.ac.kumoh.polaris.library.entity.Library
import kr.ac.kumoh.polaris.library.implement.LibraryReader
import kr.ac.kumoh.polaris.library.repository.LibraryRepository
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.implement.UserReader
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

@DataJpaTest
@Import(
    BookmarkService::class,
    UserReader::class,
    LibraryReader::class,
    BookReader::class,
    BookWriter::class,
    BookmarkServiceDataJpaTest.TestConfig::class
)
class BookmarkServiceDataJpaTest(
    @Autowired private val bookmarkService: BookmarkService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val libraryRepository: LibraryRepository,
    @Autowired private val bookRepository: BookRepository,
    @Autowired private val libraryBookmarkRepository: LibraryBookmarkRepository,
    @Autowired private val bookBookmarkRepository: BookBookmarkRepository
) {
    @Test
    fun `bookmark library and list libraries in latest order`() {
        val user = userRepository.save(testUser("library-list"))
        val firstLibrary = libraryRepository.save(testLibrary("중앙도서관", "1001"))
        val secondLibrary = libraryRepository.save(testLibrary("시립도서관", "1002"))

        bookmarkService.bookmarkLibrary(user.id!!, firstLibrary.id!!)
        bookmarkService.bookmarkLibrary(user.id!!, secondLibrary.id!!)

        val result = bookmarkService.getBookmarkedLibraries(user.id!!)

        assertEquals(listOf(secondLibrary.id, firstLibrary.id), result.map { it.libraryId })
        assertEquals(2, libraryBookmarkRepository.count())
    }

    @Test
    fun `duplicate library bookmark throws conflict`() {
        val user = userRepository.save(testUser("library-duplicate"))
        val library = libraryRepository.save(testLibrary("양포도서관", "2001"))

        bookmarkService.bookmarkLibrary(user.id!!, library.id!!)

        val exception = assertThrows<ServiceException> {
            bookmarkService.bookmarkLibrary(user.id!!, library.id!!)
        }

        assertEquals(ErrorCode.BOOKMARK_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `bookmark book list and unbookmark book`() {
        val user = userRepository.save(testUser("book-flow"))
        val firstBook = bookRepository.save(testBook("9791191111111", "첫 번째 책"))
        val secondBook = bookRepository.save(testBook("9791192222222", "두 번째 책"))

        bookmarkService.bookmarkBook(user.id!!, firstBook.isbn!!)
        bookmarkService.bookmarkBook(user.id!!, secondBook.isbn!!)

        val bookmarkedBooks = bookmarkService.getBookmarkedBooks(user.id!!)

        assertEquals(listOf(secondBook.isbn, firstBook.isbn), bookmarkedBooks.map { it.isbn })

        bookmarkService.unbookmarkBook(user.id!!, secondBook.isbn!!)

        val remainingBooks = bookmarkService.getBookmarkedBooks(user.id!!)

        assertEquals(listOf(firstBook.isbn), remainingBooks.map { it.isbn })
        assertEquals(1, bookBookmarkRepository.count())
    }

    @Test
    fun `duplicate book bookmark throws conflict`() {
        val user = userRepository.save(testUser("book-duplicate"))
        val book = bookRepository.save(testBook("9791193333333", "중복 책"))

        bookmarkService.bookmarkBook(user.id!!, book.isbn!!)

        val exception = assertThrows<ServiceException> {
            bookmarkService.bookmarkBook(user.id!!, book.isbn!!)
        }

        assertEquals(ErrorCode.BOOKMARK_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `unbookmark library is idempotent when bookmark is missing`() {
        val user = userRepository.save(testUser("library-idempotent"))
        val library = libraryRepository.save(testLibrary("상모도서관", "3001"))

        bookmarkService.unbookmarkLibrary(user.id!!, library.id!!)

        assertTrue(libraryBookmarkRepository.findAll().isEmpty())
    }

    private fun testUser(suffix: String): User = User(
        provider = UserAuthProvider.KAKAO,
        oidcIssuer = "https://kauth.kakao.com",
        oidcSubject = "subject-$suffix",
        nickname = "tester-$suffix",
        email = "tester-$suffix@example.com"
    )

    private fun testLibrary(name: String, libCode: String): Library = Library(
        name = name,
        address = Address(
            province = "경북",
            city = "구미시",
            detail = name
        ),
        libCode = libCode
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
        fun naverSearchApiProperties(): NaverSearchApiProperties = NaverSearchApiProperties(
            list = NaverSearchApiDetailProperties(
                baseUrl = "http://localhost",
                clientId = "test-client-id",
                clientSecret = "test-client-secret"
            ),
            detail = NaverSearchApiDetailProperties(
                baseUrl = "http://localhost",
                clientId = "test-client-id",
                clientSecret = "test-client-secret"
            )
        )

        @Bean
        fun naverSearchDetailRestClient(): RestClient =
            RestClient.builder()
                .baseUrl("http://localhost")
                .build()

        @Bean
        fun naverSearchBookDetailClient(
            naverSearchDetailRestClient: RestClient,
            naverSearchApiProperties: NaverSearchApiProperties
        ): NaverSearchBookDetailClient =
            NaverSearchBookDetailClient(naverSearchDetailRestClient, naverSearchApiProperties)

        @Bean
        fun bookMetadataLoader(naverSearchBookDetailClient: NaverSearchBookDetailClient): BookMetadataLoader =
            BookMetadataLoader(naverSearchBookDetailClient)
    }
}
