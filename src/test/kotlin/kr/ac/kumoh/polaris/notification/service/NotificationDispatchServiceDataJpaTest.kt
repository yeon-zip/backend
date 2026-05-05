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
import kr.ac.kumoh.polaris.notification.entity.PushPlatform
import kr.ac.kumoh.polaris.notification.entity.UserPushToken
import kr.ac.kumoh.polaris.notification.implement.BookAvailableNotificationPayload
import kr.ac.kumoh.polaris.notification.implement.NotificationSendResult
import kr.ac.kumoh.polaris.notification.implement.NotificationSender
import kr.ac.kumoh.polaris.notification.repository.NotificationSubscriptionRepository
import kr.ac.kumoh.polaris.notification.repository.UserPushTokenRepository
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.web.reactive.function.client.WebClient

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
    @Autowired private val userPushTokenRepository: UserPushTokenRepository,
    @Autowired private val notificationSubscriptionRepository: NotificationSubscriptionRepository,
    @Autowired private val fakeChecker: FakeLibraryBookAvailabilityChecker,
    @Autowired private val fakeSender: FakeNotificationSender
) {
    @BeforeEach
    fun resetFakes() {
        fakeChecker.resultsByIsbnAndLibCode.clear()
        fakeChecker.calls.clear()
        fakeSender.handler = { tokens, _ -> NotificationSendResult(tokens.toSet(), emptySet(), emptySet()) }
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

        notificationDispatchService.dispatchOnce()

        assertEquals(1, fakeChecker.calls.size)
        assertEquals(book.isbn, fakeChecker.calls.single().isbn)
        assertEquals(setOf("L1", "L2"), fakeChecker.calls.single().libCodes.toSet())
    }

    @Test
    fun `UNKNOWN preserves stable availability state`() {
        val subscription = savedSubscription("unknown")
        subscription.recordNotAvailable(AlertCheckOutcome.ON_LOAN, java.time.LocalDateTime.now())
        fakeChecker.setResult(subscription.book.isbn!!, subscription.library.libCode, unknown())

        notificationDispatchService.dispatchOnce()

        val reloaded = notificationSubscriptionRepository.findAll().single()
        assertEquals(AlertAvailabilityState.NOT_AVAILABLE, reloaded.lastStableAvailability)
        assertEquals(AlertCheckOutcome.UNKNOWN, reloaded.lastCheckOutcome)
    }

    @Test
    fun `send failure does not advance subscription availability or notified time`() {
        val subscription = savedSubscription("send-failure")
        userPushTokenRepository.save(UserPushToken(subscription.user, PushPlatform.ANDROID, "token-a"))
        fakeChecker.setResult(subscription.book.isbn!!, subscription.library.libCode, available())
        fakeSender.handler = { tokens, _ -> NotificationSendResult(emptySet(), emptySet(), tokens.toSet(), totalFailure = true) }

        notificationDispatchService.dispatchOnce()

        val reloaded = notificationSubscriptionRepository.findAll().single()
        assertEquals(AlertAvailabilityState.UNKNOWN, reloaded.lastStableAvailability)
        assertNull(reloaded.lastNotifiedAt)
        assertNotNull(reloaded.lastDispatchErrorAt)
    }

    @Test
    fun `permanent invalid token is soft deactivated while successful token advances subscription`() {
        val subscription = savedSubscription("invalid-token")
        userPushTokenRepository.save(UserPushToken(subscription.user, PushPlatform.ANDROID, "valid-token"))
        userPushTokenRepository.save(UserPushToken(subscription.user, PushPlatform.IOS, "invalid-token"))
        fakeChecker.setResult(subscription.book.isbn!!, subscription.library.libCode, available())
        fakeSender.handler = { _, _ ->
            NotificationSendResult(
                successes = setOf("valid-token"),
                permanentFailures = setOf("invalid-token"),
                transientFailures = emptySet()
            )
        }

        notificationDispatchService.dispatchOnce()

        val tokens = userPushTokenRepository.findAll().associateBy { it.deviceToken }
        assertTrue(tokens.getValue("valid-token").active)
        assertFalse(tokens.getValue("invalid-token").active)
        assertEquals("FCM_INVALID", tokens.getValue("invalid-token").deactivationReason)
        assertEquals(AlertAvailabilityState.AVAILABLE, notificationSubscriptionRepository.findAll().single().lastStableAvailability)
    }

    @Test
    fun `total-call exception does not deactivate all tokens or advance subscription`() {
        val subscription = savedSubscription("total-exception")
        userPushTokenRepository.save(UserPushToken(subscription.user, PushPlatform.ANDROID, "token-a"))
        userPushTokenRepository.save(UserPushToken(subscription.user, PushPlatform.IOS, "token-b"))
        fakeChecker.setResult(subscription.book.isbn!!, subscription.library.libCode, available())
        fakeSender.handler = { _, _ -> throw IllegalStateException("FCM down") }

        notificationDispatchService.dispatchOnce()

        assertTrue(userPushTokenRepository.findAll().all { it.active })
        val reloaded = notificationSubscriptionRepository.findAll().single()
        assertEquals(AlertAvailabilityState.UNKNOWN, reloaded.lastStableAvailability)
        assertNull(reloaded.lastNotifiedAt)
    }

    private fun savedSubscription(suffix: String): NotificationSubscription {
        val user = userRepository.save(testUser(suffix))
        val book = bookRepository.save(testBook("979119${suffix.hashCode().toString().filter(Char::isDigit).padEnd(7, '0').take(7)}"))
        val library = libraryRepository.save(testLibrary(suffix, "LIB-${suffix.take(8)}"))
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
        @Primary
        fun fakeChecker(): FakeLibraryBookAvailabilityChecker = FakeLibraryBookAvailabilityChecker()

        @Bean
        @Primary
        fun fakeSender(): FakeNotificationSender = FakeNotificationSender()
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

        fun setResult(isbn: String, libCode: String, result: LibraryBookAvailabilityResult) {
            resultsByIsbnAndLibCode[isbn to libCode] = result
        }

        override fun checkAll(libCodes: Collection<String>, isbn: String): Map<String, LibraryBookAvailabilityResult> {
            calls += AvailabilityCall(isbn = isbn, libCodes = libCodes.toList())
            return libCodes.associateWith { libCode ->
                resultsByIsbnAndLibCode[isbn to libCode] ?: LibraryBookAvailabilityResult.unknown()
            }
        }
    }

    class FakeNotificationSender : NotificationSender {
        var handler: (List<String>, BookAvailableNotificationPayload) -> NotificationSendResult =
            { tokens, _ -> NotificationSendResult(tokens.toSet(), emptySet(), emptySet()) }

        override fun sendBookAvailable(
            tokens: List<String>,
            payload: BookAvailableNotificationPayload
        ): NotificationSendResult = handler(tokens, payload)
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
