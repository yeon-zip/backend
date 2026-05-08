package kr.ac.kumoh.polaris.notification.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.global.util.IsbnNormalizer
import kr.ac.kumoh.polaris.library.implement.LibraryReader
import kr.ac.kumoh.polaris.notification.entity.NotificationSubscription
import kr.ac.kumoh.polaris.notification.repository.NotificationSubscriptionRepository
import kr.ac.kumoh.polaris.user.implement.UserReader
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationSubscriptionService(
    private val userReader: UserReader,
    private val libraryReader: LibraryReader,
    private val bookReader: BookReader,
    private val bookMetadataLoader: BookMetadataLoader,
    private val bookWriter: BookWriter,
    private val notificationSubscriptionRepository: NotificationSubscriptionRepository
) {
    @Transactional
    fun subscribe(userId: Long, isbn: String, libraryId: Long) {
        val user = userReader.findByIdOrThrow(userId)
        val book = resolveBookByIsbnOrThrow(isbn)
        val library = libraryReader.findByIdOrThrow(libraryId)
        val bookId = book.id ?: throw ServiceException(ErrorCode.BOOK_NOT_FOUND)
        val persistedLibraryId = library.id ?: throw ServiceException(ErrorCode.LIBRARY_NOT_FOUND)

        val existing = notificationSubscriptionRepository.findByUserIdAndBookIdAndLibraryId(
            userId = userId,
            bookId = bookId,
            libraryId = persistedLibraryId
        )
        if (existing != null) {
            if (existing.active) {
                throw duplicateSubscriptionException(book.isbn ?: isbn, persistedLibraryId)
            }
            existing.reactivate()
            return
        }

        try {
            notificationSubscriptionRepository.saveAndFlush(
                NotificationSubscription(
                    user = user,
                    book = book,
                    library = library
                )
            )
        } catch (_: DataIntegrityViolationException) {
            val raced = notificationSubscriptionRepository.findByUserIdAndBookIdAndLibraryId(
                userId = userId,
                bookId = bookId,
                libraryId = persistedLibraryId
            ) ?: throw duplicateSubscriptionException(book.isbn ?: isbn, persistedLibraryId)

            if (raced.active) {
                throw duplicateSubscriptionException(book.isbn ?: isbn, persistedLibraryId)
            }
            raced.reactivate()
        }
    }

    @Transactional(readOnly = true)
    fun getMySubscriptions(userId: Long): List<NotificationSubscriptionResult> {
        userReader.findByIdOrThrow(userId)

        return notificationSubscriptionRepository.findActiveByUserId(userId)
            .map(NotificationSubscriptionResult::from)
    }

    @Transactional
    fun unsubscribe(userId: Long, isbn: String, libraryId: Long) {
        userReader.findByIdOrThrow(userId)
        val normalizedIsbn = IsbnNormalizer.normalize(isbn)
        val book = bookReader.findByIsbn(normalizedIsbn) ?: return
        val bookId = book.id ?: throw ServiceException(ErrorCode.BOOK_NOT_FOUND)

        notificationSubscriptionRepository.findByUserIdAndBookIdAndLibraryId(userId, bookId, libraryId)
            ?.deactivate()
    }

    private fun resolveBookByIsbnOrThrow(isbn: String): Book {
        val normalizedIsbn = IsbnNormalizer.normalize(isbn)

        return bookReader.findByIsbn(normalizedIsbn)
            ?: bookMetadataLoader.loadByIsbn(normalizedIsbn)
                ?.let(bookWriter::saveIfAbsent)
            ?: throw ServiceException(
                errorCode = ErrorCode.BOOK_NOT_FOUND,
                message = "도서를 찾을 수 없습니다. isbn=$normalizedIsbn"
            )
    }

    private fun duplicateSubscriptionException(isbn: String, libraryId: Long): ServiceException =
        ServiceException(
            errorCode = ErrorCode.NOTIFICATION_SUBSCRIPTION_ALREADY_EXISTS,
            message = "이미 등록된 알림 구독입니다. isbn=$isbn, libraryId=$libraryId"
        )
}

data class NotificationSubscriptionResult(
    val subscriptionId: Long,
    val isbn: String,
    val title: String,
    val author: String?,
    val coverImageUrl: String?,
    val libraryId: Long,
    val libraryName: String,
    val lastStableAvailability: String,
    val lastCheckOutcome: String,
    val lastCheckedAt: LocalDateTime?,
    val lastNotifiedAt: LocalDateTime?
) {
    companion object {
        fun from(subscription: NotificationSubscription): NotificationSubscriptionResult {
            val book = subscription.book
            val library = subscription.library
            return NotificationSubscriptionResult(
                subscriptionId = subscription.id ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE),
                isbn = book.isbn ?: throw ServiceException(ErrorCode.BOOK_NOT_FOUND),
                title = book.title,
                author = book.author,
                coverImageUrl = book.coverImageUrl,
                libraryId = library.id ?: throw ServiceException(ErrorCode.LIBRARY_NOT_FOUND),
                libraryName = library.name,
                lastStableAvailability = subscription.lastStableAvailability.name,
                lastCheckOutcome = subscription.lastCheckOutcome.name,
                lastCheckedAt = subscription.lastCheckedAt,
                lastNotifiedAt = subscription.lastNotifiedAt
            )
        }
    }
}
