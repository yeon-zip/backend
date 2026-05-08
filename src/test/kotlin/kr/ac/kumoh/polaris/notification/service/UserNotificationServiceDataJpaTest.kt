package kr.ac.kumoh.polaris.notification.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.repository.BookRepository
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.library.entity.Address
import kr.ac.kumoh.polaris.library.entity.Library
import kr.ac.kumoh.polaris.library.repository.LibraryRepository
import kr.ac.kumoh.polaris.notification.entity.NotificationSubscription
import kr.ac.kumoh.polaris.notification.entity.NotificationType
import kr.ac.kumoh.polaris.notification.entity.UserNotification
import kr.ac.kumoh.polaris.notification.repository.NotificationSubscriptionRepository
import kr.ac.kumoh.polaris.notification.repository.UserNotificationRepository
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.implement.UserReader
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@Import(UserNotificationService::class, UserReader::class)
class UserNotificationServiceDataJpaTest(
    @Autowired private val userNotificationService: UserNotificationService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val bookRepository: BookRepository,
    @Autowired private val libraryRepository: LibraryRepository,
    @Autowired private val notificationSubscriptionRepository: NotificationSubscriptionRepository,
    @Autowired private val userNotificationRepository: UserNotificationRepository
) {
    @Test
    fun `count excludes soft deleted notifications`() {
        val user = userRepository.save(testUser("count"))
        val subscription = savedSubscription(user, "9791191111111", "count-lib")
        val visible = userNotificationRepository.save(testNotification(subscription, createdAt = LocalDateTime.parse("2026-05-07T09:00:00")))
        val deleted = userNotificationRepository.save(testNotification(subscription, notificationDate = LocalDate.of(2026, 5, 8)))
        deleted.softDelete(LocalDateTime.parse("2026-05-08T10:00:00"))
        userNotificationRepository.flush()

        val count = userNotificationService.countVisible(user.id!!)

        assertEquals(1, count)
        assertTrue(userNotificationRepository.findById(visible.id!!).get().deletedAt == null)
    }

    @Test
    fun `list returns latest id desc and applies cursor`() {
        val user = userRepository.save(testUser("list"))
        val first = userNotificationRepository.save(testNotification(savedSubscription(user, "9791192222221", "list-a")))
        val second = userNotificationRepository.save(testNotification(savedSubscription(user, "9791192222222", "list-b")))
        val third = userNotificationRepository.save(testNotification(savedSubscription(user, "9791192222223", "list-c")))

        val firstPage = userNotificationService.getVisibleNotifications(user.id!!, cursor = null, limit = 2)
        val secondPage = userNotificationService.getVisibleNotifications(user.id!!, cursor = firstPage.nextCursor, limit = 2)

        assertEquals(listOf(third.id, second.id), firstPage.items.map { it.notificationId })
        assertEquals(true, firstPage.hasNext)
        assertEquals(second.id.toString(), firstPage.nextCursor)
        assertEquals(listOf(first.id), secondPage.items.map { it.notificationId })
        assertEquals(false, secondPage.hasNext)
    }

    @Test
    fun `invalid list inputs throw invalid input`() {
        val user = userRepository.save(testUser("invalid"))

        assertEquals(
            ErrorCode.INVALID_INPUT_VALUE,
            assertThrows<ServiceException> {
                userNotificationService.getVisibleNotifications(user.id!!, cursor = "abc", limit = 20)
            }.errorCode
        )
        assertEquals(
            ErrorCode.INVALID_INPUT_VALUE,
            assertThrows<ServiceException> {
                userNotificationService.getVisibleNotifications(user.id!!, cursor = null, limit = 101)
            }.errorCode
        )
    }

    @Test
    fun `delete soft deletes owned notification and already deleted owned notification is no-op`() {
        val user = userRepository.save(testUser("delete"))
        val notification = userNotificationRepository.save(testNotification(savedSubscription(user, "9791193333333", "delete-lib")))

        userNotificationService.deleteNotification(user.id!!, notification.id!!)
        userNotificationService.deleteNotification(user.id!!, notification.id!!)

        val reloaded = userNotificationRepository.findById(notification.id!!).get()
        assertTrue(reloaded.deletedAt != null)
        assertEquals(0, userNotificationService.countVisible(user.id!!))
    }

    @Test
    fun `delete missing notification returns not found`() {
        val user = userRepository.save(testUser("missing"))

        val exception = assertThrows<ServiceException> {
            userNotificationService.deleteNotification(user.id!!, 999_999L)
        }

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `delete other user's notification returns forbidden`() {
        val owner = userRepository.save(testUser("owner"))
        val other = userRepository.save(testUser("other"))
        val notification = userNotificationRepository.save(testNotification(savedSubscription(owner, "9791194444444", "owner-lib")))

        val exception = assertThrows<ServiceException> {
            userNotificationService.deleteNotification(other.id!!, notification.id!!)
        }

        assertEquals(ErrorCode.NOTIFICATION_ACCESS_DENIED, exception.errorCode)
        assertNull(userNotificationRepository.findById(notification.id!!).get().deletedAt)
    }

    private fun savedSubscription(user: User, isbn: String, suffix: String): NotificationSubscription {
        val book = bookRepository.save(Book(isbn = isbn, title = "도서 $isbn"))
        val library = libraryRepository.save(testLibrary(suffix))
        return notificationSubscriptionRepository.save(NotificationSubscription(user, book, library))
    }

    private fun testNotification(
        subscription: NotificationSubscription,
        notificationDate: LocalDate = LocalDate.of(2026, 5, 7),
        createdAt: LocalDateTime = LocalDateTime.parse("2026-05-07T09:00:00")
    ): UserNotification = UserNotification(
        user = subscription.user,
        subscription = subscription,
        book = subscription.book,
        library = subscription.library,
        notificationType = NotificationType.BOOK_AVAILABLE,
        title = "대출 가능 알림",
        message = "알림 받기 한 도서가 ${subscription.library.name}에서 대출 가능합니다.",
        notificationDate = notificationDate,
        createdAt = createdAt
    )

    private fun testUser(suffix: String): User = User(
        provider = UserAuthProvider.KAKAO,
        oidcIssuer = "https://kauth.kakao.com",
        oidcSubject = "notification-$suffix",
        nickname = "notification-$suffix"
    )

    private fun testLibrary(suffix: String): Library = Library(
        name = "도서관 $suffix",
        address = Address(province = "경북", city = "구미시", detail = suffix),
        libCode = "lib-$suffix"
    )
}
