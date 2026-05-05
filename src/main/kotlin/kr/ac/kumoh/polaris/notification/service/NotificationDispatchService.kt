package kr.ac.kumoh.polaris.notification.service

import kr.ac.kumoh.polaris.bookavailability.implement.LibraryBookAvailabilityChecker
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityStatus
import kr.ac.kumoh.polaris.notification.config.NotificationProperties
import kr.ac.kumoh.polaris.notification.entity.AlertAvailabilityState
import kr.ac.kumoh.polaris.notification.entity.AlertCheckOutcome
import kr.ac.kumoh.polaris.notification.entity.NotificationSubscription
import kr.ac.kumoh.polaris.notification.implement.BookAvailableNotificationPayload
import kr.ac.kumoh.polaris.notification.implement.NotificationSender
import kr.ac.kumoh.polaris.notification.repository.NotificationSubscriptionRepository
import kr.ac.kumoh.polaris.notification.repository.UserPushTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationDispatchService(
    private val notificationSubscriptionRepository: NotificationSubscriptionRepository,
    private val userPushTokenRepository: UserPushTokenRepository,
    private val libraryBookAvailabilityChecker: LibraryBookAvailabilityChecker,
    private val notificationSender: NotificationSender,
    private val notificationProperties: NotificationProperties
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun dispatchOnce(now: LocalDateTime = LocalDateTime.now()): NotificationDispatchResult {
        val batchSize = notificationProperties.dispatch.batchSize.coerceIn(1, 500)
        var afterId = 0L
        var scanned = 0
        var sent = 0
        var permanentTokenFailures = 0
        var unknown = 0
        var sendFailures = 0

        while (true) {
            val page = notificationSubscriptionRepository.findActivePageAfterId(
                afterId = afterId,
                pageable = PageRequest.of(0, batchSize)
            )
            if (page.isEmpty()) {
                break
            }

            scanned += page.size
            afterId = page.last().id ?: afterId

            val availabilityByKey = resolveAvailability(page)
            val activeTokensByUserId = loadActiveTokensByUserId(page)

            for (subscription in page) {
                val key = AvailabilityKey(
                    isbn = subscription.book.isbn ?: continue,
                    libCode = subscription.library.libCode
                )
                val availability = availabilityByKey[key] ?: LibraryBookAvailabilityResult.unknown()

                when (availability.status) {
                    LibraryBookAvailabilityStatus.UNKNOWN -> {
                        subscription.recordUnknown(now)
                        unknown++
                    }
                    LibraryBookAvailabilityStatus.ON_LOAN ->
                        subscription.recordNotAvailable(AlertCheckOutcome.ON_LOAN, now)
                    LibraryBookAvailabilityStatus.UNAVAILABLE ->
                        subscription.recordNotAvailable(AlertCheckOutcome.UNAVAILABLE, now)
                    LibraryBookAvailabilityStatus.AVAILABLE -> {
                        if (subscription.lastStableAvailability == AlertAvailabilityState.AVAILABLE) {
                            subscription.recordAvailableObserved(now)
                            continue
                        }

                        val tokens = activeTokensByUserId[subscription.user.id]
                            .orEmpty()
                            .map { it.deviceToken }
                        val subscriptionId = subscription.id ?: continue
                        val libraryId = subscription.library.id ?: continue
                        val result = try {
                            notificationSender.sendBookAvailable(
                                tokens = tokens,
                                payload = BookAvailableNotificationPayload(
                                    isbn = key.isbn,
                                    libraryId = libraryId,
                                    subscriptionId = subscriptionId
                                )
                            )
                        } catch (exception: Exception) {
                            log.error(
                                "Notification sender failed for subscriptionId={}; preserving token and subscription state",
                                subscriptionId,
                                exception
                            )
                            kr.ac.kumoh.polaris.notification.implement.NotificationSendResult.totalFailure(tokens)
                        }

                        if (result.permanentFailures.isNotEmpty()) {
                            activeTokensByUserId[subscription.user.id].orEmpty()
                                .filter { it.deviceToken in result.permanentFailures }
                                .forEach { it.deactivate("FCM_INVALID") }
                            permanentTokenFailures += result.permanentFailures.size
                        }

                        if (result.hasSuccess && !result.totalFailure) {
                            subscription.recordAvailableNotified(now)
                            sent++
                        } else {
                            subscription.recordDispatchFailure(now)
                            sendFailures++
                        }
                    }
                }
            }

            if (page.size < batchSize) {
                break
            }
        }

        return NotificationDispatchResult(
            scannedSubscriptions = scanned,
            sentNotifications = sent,
            permanentTokenFailures = permanentTokenFailures,
            unknownOutcomes = unknown,
            sendFailures = sendFailures
        )
    }

    private fun resolveAvailability(
        subscriptions: List<NotificationSubscription>
    ): Map<AvailabilityKey, LibraryBookAvailabilityResult> =
        subscriptions
            .mapNotNull { subscription ->
                val isbn = subscription.book.isbn ?: return@mapNotNull null
                AvailabilityKey(isbn = isbn, libCode = subscription.library.libCode)
            }
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

    private fun loadActiveTokensByUserId(
        subscriptions: List<NotificationSubscription>
    ) = userPushTokenRepository.findActiveByUserIdIn(
        subscriptions.mapNotNull { it.user.id }.distinct()
    ).groupBy { it.user.id }

    private data class AvailabilityKey(
        val isbn: String,
        val libCode: String
    )
}

data class NotificationDispatchResult(
    val scannedSubscriptions: Int,
    val sentNotifications: Int,
    val permanentTokenFailures: Int,
    val unknownOutcomes: Int,
    val sendFailures: Int
)
