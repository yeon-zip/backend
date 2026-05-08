package kr.ac.kumoh.polaris.notification.service

import kr.ac.kumoh.polaris.bookavailability.implement.LibraryBookAvailabilityChecker
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityStatus
import kr.ac.kumoh.polaris.notification.config.NotificationProperties
import kr.ac.kumoh.polaris.notification.entity.AlertAvailabilityState
import kr.ac.kumoh.polaris.notification.entity.AlertCheckOutcome
import kr.ac.kumoh.polaris.notification.entity.NotificationSubscription
import kr.ac.kumoh.polaris.notification.entity.NotificationType
import kr.ac.kumoh.polaris.notification.entity.UserNotification
import kr.ac.kumoh.polaris.notification.repository.NotificationSubscriptionRepository
import kr.ac.kumoh.polaris.notification.repository.UserNotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class NotificationDispatchService(
    private val notificationSubscriptionRepository: NotificationSubscriptionRepository,
    private val userNotificationRepository: UserNotificationRepository,
    private val libraryBookAvailabilityChecker: LibraryBookAvailabilityChecker,
    private val notificationProperties: NotificationProperties,
    private val transactionTemplate: TransactionTemplate
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun dispatchOnce(now: LocalDateTime = dispatchNow()): NotificationDispatchResult {
        val batchSize = notificationProperties.dispatch.batchSize.coerceIn(1, 500)
        val notificationDate = now.toLocalDate()
        var afterId = 0L
        var scanned = 0
        var created = 0
        var unknown = 0
        var failed = 0

        while (true) {
            val page = notificationSubscriptionRepository.findActivePageAfterId(
                afterId = afterId,
                pageable = PageRequest.of(0, batchSize)
            )
            if (page.isEmpty()) break

            scanned += page.size
            afterId = page.last().id ?: afterId

            val snapshots = page.mapNotNull(SubscriptionSnapshot::from)
            val availabilityByKey = resolveAvailability(snapshots)

            for (snapshot in snapshots) {
                val availability = availabilityByKey[AvailabilityKey(snapshot.isbn, snapshot.libCode)]
                    ?: LibraryBookAvailabilityResult.unknown()
                try {
                    val result = processSubscription(
                        snapshot = snapshot,
                        availability = availability,
                        notificationDate = notificationDate,
                        now = now
                    )
                    if (result.createdNotification) created++
                    if (result.unknownOutcome) unknown++
                } catch (exception: Exception) {
                    failed++
                    recordFailure(snapshot.subscriptionId, now)
                    log.error(
                        "Notification subscription processing failed - subscriptionId={}",
                        snapshot.subscriptionId,
                        exception
                    )
                }
            }

            if (page.size < batchSize) break
        }

        return NotificationDispatchResult(
            scannedSubscriptions = scanned,
            createdNotifications = created,
            unknownOutcomes = unknown,
            failedSubscriptions = failed
        )
    }

    private fun dispatchNow(): LocalDateTime =
        LocalDateTime.now(ZoneId.of(notificationProperties.dispatch.zone))

    private fun resolveAvailability(
        subscriptions: List<SubscriptionSnapshot>
    ): Map<AvailabilityKey, LibraryBookAvailabilityResult> =
        subscriptions
            .map { AvailabilityKey(isbn = it.isbn, libCode = it.libCode) }
            .distinct()
            .groupBy { it.isbn }
            .flatMap { (isbn, keys) ->
                val availabilityByLibCode = try {
                    libraryBookAvailabilityChecker.checkAll(
                        libCodes = keys.map { it.libCode }.distinct(),
                        isbn = isbn
                    )
                } catch (exception: Exception) {
                    log.error("Notification availability check failed - isbn={}", isbn, exception)
                    emptyMap()
                }

                keys.map { key ->
                    key to (availabilityByLibCode[key.libCode] ?: LibraryBookAvailabilityResult.unknown())
                }
            }
            .toMap()

    private fun processSubscription(
        snapshot: SubscriptionSnapshot,
        availability: LibraryBookAvailabilityResult,
        notificationDate: LocalDate,
        now: LocalDateTime
    ): SubscriptionProcessResult =
        transactionTemplate.execute {
            val subscription = notificationSubscriptionRepository.findByIdWithUserBookLibrary(snapshot.subscriptionId)
                ?: return@execute SubscriptionProcessResult()
            if (!subscription.active) {
                return@execute SubscriptionProcessResult()
            }

            when (availability.status) {
                LibraryBookAvailabilityStatus.UNKNOWN -> {
                    subscription.recordUnknown(now)
                    SubscriptionProcessResult(unknownOutcome = true)
                }
                LibraryBookAvailabilityStatus.ON_LOAN -> {
                    subscription.recordNotAvailable(AlertCheckOutcome.ON_LOAN, now)
                    SubscriptionProcessResult()
                }
                LibraryBookAvailabilityStatus.UNAVAILABLE -> {
                    subscription.recordNotAvailable(AlertCheckOutcome.UNAVAILABLE, now)
                    SubscriptionProcessResult()
                }
                LibraryBookAvailabilityStatus.AVAILABLE -> processAvailable(subscription, notificationDate, now)
            }
        } ?: SubscriptionProcessResult()

    private fun processAvailable(
        subscription: NotificationSubscription,
        notificationDate: LocalDate,
        now: LocalDateTime
    ): SubscriptionProcessResult {
        if (subscription.lastStableAvailability == AlertAvailabilityState.AVAILABLE) {
            subscription.recordAvailableObserved(now)
            return SubscriptionProcessResult()
        }

        val created = createNotificationIfAbsent(subscription, notificationDate, now)
        subscription.recordAvailableNotified(now)
        return SubscriptionProcessResult(createdNotification = created)
    }

    private fun createNotificationIfAbsent(
        subscription: NotificationSubscription,
        notificationDate: LocalDate,
        now: LocalDateTime
    ): Boolean {
        val subscriptionId = subscription.id ?: return false
        if (userNotificationRepository.existsBySubscriptionIdAndNotificationTypeAndNotificationDate(
                subscriptionId = subscriptionId,
                notificationType = NotificationType.BOOK_AVAILABLE,
                notificationDate = notificationDate
            )
        ) {
            return false
        }

        val library = subscription.library
        return try {
            userNotificationRepository.saveAndFlush(
                UserNotification(
                    user = subscription.user,
                    subscription = subscription,
                    book = subscription.book,
                    library = library,
                    notificationType = NotificationType.BOOK_AVAILABLE,
                    title = BOOK_AVAILABLE_TITLE,
                    message = "알림 받기 한 도서가 ${library.name}에서 대출 가능합니다.",
                    notificationDate = notificationDate,
                    createdAt = now
                )
            )
            true
        } catch (_: DataIntegrityViolationException) {
            log.info("Duplicate notification creation skipped - subscriptionId={}, notificationDate={}", subscriptionId, notificationDate)
            false
        }
    }

    private fun recordFailure(subscriptionId: Long, now: LocalDateTime) {
        try {
            transactionTemplate.execute {
                notificationSubscriptionRepository.findByIdWithUserBookLibrary(subscriptionId)
                    ?.recordDispatchFailure(now)
            }
        } catch (exception: Exception) {
            log.error("Notification subscription failure state update failed - subscriptionId={}", subscriptionId, exception)
        }
    }

    private data class AvailabilityKey(
        val isbn: String,
        val libCode: String
    )

    private data class SubscriptionSnapshot(
        val subscriptionId: Long,
        val isbn: String,
        val libCode: String
    ) {
        companion object {
            fun from(subscription: NotificationSubscription): SubscriptionSnapshot? {
                val subscriptionId = subscription.id ?: return null
                val isbn = subscription.book.isbn ?: return null
                return SubscriptionSnapshot(
                    subscriptionId = subscriptionId,
                    isbn = isbn,
                    libCode = subscription.library.libCode
                )
            }
        }
    }

    private data class SubscriptionProcessResult(
        val createdNotification: Boolean = false,
        val unknownOutcome: Boolean = false
    )

    companion object {
        private const val BOOK_AVAILABLE_TITLE = "대출 가능 알림"
    }
}

data class NotificationDispatchResult(
    val scannedSubscriptions: Int,
    val createdNotifications: Int,
    val unknownOutcomes: Int,
    val failedSubscriptions: Int
)
