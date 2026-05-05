package kr.ac.kumoh.polaris.notification.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.book.repository.BookRepository
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.library.entity.Address
import kr.ac.kumoh.polaris.library.entity.Library
import kr.ac.kumoh.polaris.library.implement.LibraryReader
import kr.ac.kumoh.polaris.library.repository.LibraryRepository
import kr.ac.kumoh.polaris.notification.repository.NotificationSubscriptionRepository
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.implement.UserReader
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean

@DataJpaTest
@Import(
    NotificationSubscriptionService::class,
    UserReader::class,
    LibraryReader::class,
    BookReader::class,
    BookWriter::class
)
class NotificationSubscriptionServiceDataJpaTest(
    @Autowired private val notificationSubscriptionService: NotificationSubscriptionService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val bookRepository: BookRepository,
    @Autowired private val libraryRepository: LibraryRepository,
    @Autowired private val notificationSubscriptionRepository: NotificationSubscriptionRepository
) {
    @MockitoBean
    private lateinit var bookMetadataLoader: BookMetadataLoader

    @Test
    fun `subscription create stores active subscription`() {
        val user = userRepository.save(testUser("create"))
        val book = bookRepository.save(testBook("9791191111111"))
        val library = libraryRepository.save(testLibrary("create-lib"))

        notificationSubscriptionService.subscribe(user.id!!, book.isbn!!, library.id!!)

        val subscription = notificationSubscriptionRepository.findAll().single()
        assertTrue(subscription.active)
        assertEquals(user.id, subscription.user.id)
        assertEquals(book.id, subscription.book.id)
        assertEquals(library.id, subscription.library.id)
    }

    @Test
    fun `active duplicate subscription throws conflict`() {
        val user = userRepository.save(testUser("duplicate"))
        val book = bookRepository.save(testBook("9791192222222"))
        val library = libraryRepository.save(testLibrary("duplicate-lib"))

        notificationSubscriptionService.subscribe(user.id!!, book.isbn!!, library.id!!)

        val exception = assertThrows<ServiceException> {
            notificationSubscriptionService.subscribe(user.id!!, book.isbn!!, library.id!!)
        }
        assertEquals(ErrorCode.NOTIFICATION_SUBSCRIPTION_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `inactive subscription is reactivated`() {
        val user = userRepository.save(testUser("reactivate"))
        val book = bookRepository.save(testBook("9791193333333"))
        val library = libraryRepository.save(testLibrary("reactivate-lib"))

        notificationSubscriptionService.subscribe(user.id!!, book.isbn!!, library.id!!)
        notificationSubscriptionService.unsubscribe(user.id!!, book.isbn!!, library.id!!)
        assertFalse(notificationSubscriptionRepository.findAll().single().active)

        notificationSubscriptionService.subscribe(user.id!!, book.isbn!!, library.id!!)

        assertEquals(1, notificationSubscriptionRepository.count())
        assertTrue(notificationSubscriptionRepository.findAll().single().active)
    }

    @Test
    fun `subscription delete is idempotent for missing subscription`() {
        val user = userRepository.save(testUser("delete-missing"))
        val book = bookRepository.save(testBook("9791194444444"))
        val library = libraryRepository.save(testLibrary("delete-missing-lib"))

        notificationSubscriptionService.unsubscribe(user.id!!, book.isbn!!, library.id!!)

        assertTrue(notificationSubscriptionRepository.findAll().isEmpty())
    }

    @Test
    fun `subscription delete is idempotent for nonexistent library id`() {
        val user = userRepository.save(testUser("delete-nonexistent-library"))
        val book = bookRepository.save(testBook("9791195555555"))
        val nonexistentLibraryId = 999_999L

        notificationSubscriptionService.unsubscribe(user.id!!, book.isbn!!, nonexistentLibraryId)

        assertTrue(notificationSubscriptionRepository.findAll().isEmpty())
    }

    @Test
    fun `subscription delete is idempotent for not owned subscription`() {
        val owner = userRepository.save(testUser("delete-owner"))
        val otherUser = userRepository.save(testUser("delete-other"))
        val book = bookRepository.save(testBook("9791196666666"))
        val library = libraryRepository.save(testLibrary("delete-not-owned-lib"))

        notificationSubscriptionService.subscribe(owner.id!!, book.isbn!!, library.id!!)
        notificationSubscriptionService.unsubscribe(otherUser.id!!, book.isbn!!, library.id!!)

        assertTrue(notificationSubscriptionRepository.findAll().single().active)
    }

    @Test
    fun `subscription delete is idempotent for inactive subscription`() {
        val user = userRepository.save(testUser("delete-inactive"))
        val book = bookRepository.save(testBook("9791197777777"))
        val library = libraryRepository.save(testLibrary("delete-inactive-lib"))

        notificationSubscriptionService.subscribe(user.id!!, book.isbn!!, library.id!!)
        notificationSubscriptionService.unsubscribe(user.id!!, book.isbn!!, library.id!!)
        notificationSubscriptionService.unsubscribe(user.id!!, book.isbn!!, library.id!!)

        assertFalse(notificationSubscriptionRepository.findAll().single().active)
    }

    @Test
    fun `subscription delete soft deactivates active subscription`() {
        val user = userRepository.save(testUser("delete-active"))
        val book = bookRepository.save(testBook("9791198888888"))
        val library = libraryRepository.save(testLibrary("delete-active-lib"))

        notificationSubscriptionService.subscribe(user.id!!, book.isbn!!, library.id!!)
        notificationSubscriptionService.unsubscribe(user.id!!, book.isbn!!, library.id!!)

        assertFalse(notificationSubscriptionRepository.findAll().single().active)
    }

    private fun testUser(suffix: String): User = User(
        provider = UserAuthProvider.KAKAO,
        oidcIssuer = "https://kauth.kakao.com",
        oidcSubject = "subscription-$suffix",
        nickname = "subscription-$suffix"
    )

    private fun testBook(isbn: String): Book = Book(
        isbn = isbn,
        title = "테스트 도서 $isbn",
        author = "테스트 저자"
    )

    private fun testLibrary(suffix: String): Library = Library(
        name = "도서관 $suffix",
        address = Address(province = "경북", city = "구미시", detail = suffix),
        libCode = "lib-$suffix"
    )
}
