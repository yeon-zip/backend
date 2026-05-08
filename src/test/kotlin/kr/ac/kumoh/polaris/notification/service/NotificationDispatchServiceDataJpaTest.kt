package kr.ac.kumoh.polaris.notification.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.repository.BookRepository
import kr.ac.kumoh.polaris.bookavailability.implement.LibraryBookAvailabilityChecker
import kr.ac.kumoh.polaris.bookavailability.implement.LibraryBookAvailabilityReader
import kr.ac.kumoh.polaris.bookavailability.implement.client.Data4LibraryBookExistClient
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityStatus
import kr.ac.kumoh.polaris.global.config.CacheConfig
import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import kr.ac.kumoh.polaris.library.entity.Library
import kr.ac.kumoh.polaris.library.repository.LibraryRepository
import kr.ac.kumoh.polaris.notification.config.NotificationProperties
import kr.ac.kumoh.polaris.notification.entity.AlertAvailabilityState
import kr.ac.kumoh.polaris.notification.entity.AlertCheckOutcome
import kr.ac.kumoh.polaris.notification.entity.NotificationSubscription
import kr.ac.kumoh.polaris.notification.repository.NotificationSubscriptionRepository
import kr.ac.kumoh.polaris.notification.repository.UserNotificationRepository
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@Import(
    NotificationDispatchService::class,
    NotificationDispatchServiceDataJpaTest.TestConfig::class
)
class NotificationDispatchServiceDataJpaTest(
    @Autowired private val notificationDispatchService: NotificationDispatchService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val bookRepository: BookRepository,
    @Autowired private val libraryRepository: LibraryRepository,
    @Autowired private val notificationSubscriptionRepository: NotificationSubscriptionRepository,
    @Autowired private val userNotificationRepository: UserNotificationRepository,
    @Autowired private val fakeChecker: FakeLibraryBookAvailabilityChecker
) {
    @BeforeEach
    fun resetFake() {
        fakeChecker.resultsByIsbnAndLibCode.clear()
        fakeChecker.calls.clear()
        fakeChecker.throwingIsbns.clear()
    }

    @Test
    fun `dispatch groups availability checks by isbn and distinct libCode`() {
        val user = userRepository.save(testUser("grouping"))
        val book = bookRepository.save(testBook("9791191111111"))
        val firstLibrary = libraryRepository.save(testLibrary("group-a", "L1"))
        val secondLibrary = libraryRepository.save(testLibrary("group-b", "L2"))
        notificationSubscriptionRepository.save(NotificationSubscription(user, book, firstLibrary))
        notificationSubscriptionRepository.save(NotificationSubscription(user, book, secondLibrary))
        fakeChecker.setResult(book.isbn!!, "L1", onLoan())
        fakeChecker.setResult(book.isbn!!, "L2", onLoan())

        notificationDispatchService.dispatchOnce(now = LocalDateTime.parse("2026-05-07T09:00:00"))

        assertEquals(1, fakeChecker.calls.size)
        assertEquals(book.isbn, fakeChecker.calls.single().isbn)
        assertEquals(setOf("L1", "L2"), fakeChecker.calls.single().libCodes.toSet())
    }

    @Test
    fun `not available to available transition creates notification once and ongoing availability does not repeat next day`() {
        val subscription = savedSubscription("transition", "9791192222222", "L-transition")
        subscription.recordNotAvailable(AlertCheckOutcome.ON_LOAN, LocalDateTime.parse("2026-05-06T09:00:00"))
        fakeChecker.setResult(subscription.book.isbn!!, subscription.library.libCode, available())

        val first = notificationDispatchService.dispatchOnce(now = LocalDateTime.parse("2026-05-07T09:00:00"))
        val second = notificationDispatchService.dispatchOnce(now = LocalDateTime.parse("2026-05-08T09:00:00"))

        assertEquals(1, first.createdNotifications)
        assertEquals(0, second.createdNotifications)
        assertEquals(1, userNotificationRepository.count())
        val notification = userNotificationRepository.findAll().single()
        assertEquals(LocalDate.of(2026, 5, 7), notification.notificationDate)
        assertEquals("대출 가능 알림", notification.title)
    }

    @Test
    fun `same day available unavailable available second transition is suppressed`() {
        val subscription = savedSubscription("same-day", "9791193333333", "L-same")
        subscription.recordNotAvailable(AlertCheckOutcome.ON_LOAN, LocalDateTime.parse("2026-05-07T08:00:00"))
        fakeChecker.setResult(subscription.book.isbn!!, subscription.library.libCode, available())
        notificationDispatchService.dispatchOnce(now = LocalDateTime.parse("2026-05-07T09:00:00"))

        fakeChecker.setResult(subscription.book.isbn!!, subscription.library.libCode, onLoan())
        notificationDispatchService.dispatchOnce(now = LocalDateTime.parse("2026-05-07T10:00:00"))

        fakeChecker.setResult(subscription.book.isbn!!, subscription.library.libCode, available())
        val third = notificationDispatchService.dispatchOnce(now = LocalDateTime.parse("2026-05-07T11:00:00"))

        assertEquals(0, third.createdNotifications)
        assertEquals(1, userNotificationRepository.count())
        assertEquals(AlertAvailabilityState.AVAILABLE, notificationSubscriptionRepository.findAll().single().lastStableAvailability)
    }

    @Test
    fun `unknown preserves stable availability state`() {
        val subscription = savedSubscription("unknown", "9791194444444", "L-unknown")
        subscription.recordNotAvailable(AlertCheckOutcome.ON_LOAN, LocalDateTime.parse("2026-05-06T09:00:00"))
        fakeChecker.setResult(subscription.book.isbn!!, subscription.library.libCode, unknown())

        val result = notificationDispatchService.dispatchOnce(now = LocalDateTime.parse("2026-05-07T09:00:00"))

        val reloaded = notificationSubscriptionRepository.findAll().single()
        assertEquals(1, result.unknownOutcomes)
        assertEquals(AlertAvailabilityState.NOT_AVAILABLE, reloaded.lastStableAvailability)
        assertEquals(AlertCheckOutcome.UNKNOWN, reloaded.lastCheckOutcome)
        assertEquals(0, userNotificationRepository.count())
    }

    @Test
    fun `availability failure for one isbn does not stop other subscriptions`() {
        val failed = savedSubscription("failed", "9791195555555", "L-failed")
        val ok = savedSubscription("ok", "9791196666666", "L-ok")
        fakeChecker.throwingIsbns += failed.book.isbn!!
        fakeChecker.setResult(ok.book.isbn!!, ok.library.libCode, available())

        val result = notificationDispatchService.dispatchOnce(now = LocalDateTime.parse("2026-05-07T09:00:00"))

        assertEquals(2, result.scannedSubscriptions)
        assertEquals(1, result.createdNotifications)
        assertEquals(1, result.unknownOutcomes)
        assertEquals(1, userNotificationRepository.count())
        assertTrue(userNotificationRepository.findAll().single().book.isbn == ok.book.isbn)
    }

    private fun savedSubscription(suffix: String, isbn: String, libCode: String): NotificationSubscription {
        val user = userRepository.save(testUser(suffix))
        val book = bookRepository.save(testBook(isbn))
        val library = libraryRepository.save(testLibrary(suffix, libCode))
        return notificationSubscriptionRepository.save(NotificationSubscription(user, book, library))
    }

    private fun testUser(suffix: String): User = User(
        provider = UserAuthProvider.KAKAO,
        oidcIssuer = "https://kauth.kakao.com",
        oidcSubject = "dispatch-$suffix",
        nickname = "dispatch-$suffix"
    )

    private fun testBook(isbn: String): Book = Book(isbn = isbn, title = "도서 $isbn")

    private fun testLibrary(suffix: String, libCode: String): Library = Library(name = "도서관 $suffix", libCode = libCode)

    private fun available() = LibraryBookAvailabilityResult(true, true, LibraryBookAvailabilityStatus.AVAILABLE)
    private fun onLoan() = LibraryBookAvailabilityResult(true, false, LibraryBookAvailabilityStatus.ON_LOAN)
    private fun unknown() = LibraryBookAvailabilityResult.unknown()

    @TestConfiguration
    class TestConfig {
        @Bean
        fun notificationProperties(): NotificationProperties = NotificationProperties()

        @Bean
        fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate =
            TransactionTemplate(transactionManager)

        @Bean
        @Primary
        fun fakeChecker(): FakeLibraryBookAvailabilityChecker = FakeLibraryBookAvailabilityChecker()
    }

    data class AvailabilityCall(val isbn: String, val libCodes: List<String>)

    class FakeLibraryBookAvailabilityChecker : LibraryBookAvailabilityChecker(
        libraryBookAvailabilityReader = LibraryBookAvailabilityReader(
            cacheManager = configuredCacheManager(),
            data4LibraryBookExistClient = FakeData4LibraryBookExistClient()
        ),
        properties = Data4LibraryApiProperties(baseUrl = "http://example.com", authKey = "test")
    ) {
        val calls = mutableListOf<AvailabilityCall>()
        val resultsByIsbnAndLibCode = mutableMapOf<Pair<String, String>, LibraryBookAvailabilityResult>()
        val throwingIsbns = mutableSetOf<String>()

        fun setResult(isbn: String, libCode: String, result: LibraryBookAvailabilityResult) {
            resultsByIsbnAndLibCode[isbn to libCode] = result
        }

        override fun checkAll(libCodes: Collection<String>, isbn: String): Map<String, LibraryBookAvailabilityResult> {
            calls += AvailabilityCall(isbn = isbn, libCodes = libCodes.toList())
            if (isbn in throwingIsbns) {
                throw IllegalStateException("Data4Library down for $isbn")
            }
            return libCodes.associateWith { libCode ->
                resultsByIsbnAndLibCode[isbn to libCode] ?: LibraryBookAvailabilityResult.unknown()
            }
        }
    }

    class FakeData4LibraryBookExistClient : Data4LibraryBookExistClient(
        data4LibraryWebClient = WebClient.builder().baseUrl("http://example.com").build(),
        properties = Data4LibraryApiProperties(baseUrl = "http://example.com", authKey = "test")
    )

    companion object {
        private fun configuredCacheManager(): org.springframework.cache.CacheManager =
            CacheConfig().cacheManager(
                Data4LibraryApiProperties(baseUrl = "http://example.com", authKey = "test")
            ).also { cacheManager ->
                (cacheManager as SimpleCacheManager).initializeCaches()
            }
    }
}
