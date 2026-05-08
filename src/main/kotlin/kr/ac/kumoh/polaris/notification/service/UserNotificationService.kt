package kr.ac.kumoh.polaris.notification.service

import kr.ac.kumoh.polaris.global.dto.CursorPageResult
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.notification.entity.NotificationType
import kr.ac.kumoh.polaris.notification.entity.UserNotification
import kr.ac.kumoh.polaris.notification.repository.UserNotificationRepository
import kr.ac.kumoh.polaris.user.implement.UserReader
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class UserNotificationService(
    private val userReader: UserReader,
    private val userNotificationRepository: UserNotificationRepository
) {
    @Transactional(readOnly = true)
    fun countVisible(userId: Long): Long {
        userReader.findByIdOrThrow(userId)
        return userNotificationRepository.countByUserIdAndDeletedAtIsNull(userId)
    }

    @Transactional(readOnly = true)
    fun getVisibleNotifications(
        userId: Long,
        cursor: String?,
        limit: Int
    ): CursorPageResult<UserNotificationResult> {
        userReader.findByIdOrThrow(userId)
        validateLimit(limit)
        val cursorId = cursor?.let(::parseCursor)
        val page = userNotificationRepository.findVisiblePageByUserId(
            userId = userId,
            cursorId = cursorId,
            pageable = PageRequest.of(0, limit + 1)
        )
        val items = if (page.size > limit) page.take(limit) else page
        val hasNext = page.size > limit

        return CursorPageResult(
            hasNext = hasNext,
            nextCursor = if (hasNext) items.lastOrNull()?.id?.toString() else null,
            items = items.map(UserNotificationResult::from)
        )
    }

    @Transactional
    fun deleteNotification(userId: Long, notificationId: Long) {
        userReader.findByIdOrThrow(userId)
        val notification = userNotificationRepository.findById(notificationId)
            .orElseThrow {
                ServiceException(
                    errorCode = ErrorCode.NOTIFICATION_NOT_FOUND,
                    message = "알림을 찾을 수 없습니다. notificationId=$notificationId"
                )
            }

        if (notification.user.id != userId) {
            throw ServiceException(
                errorCode = ErrorCode.NOTIFICATION_ACCESS_DENIED,
                message = "다른 사용자의 알림에는 접근할 수 없습니다. notificationId=$notificationId"
            )
        }

        notification.softDelete()
    }

    private fun validateLimit(limit: Int) {
        if (limit !in 1..100) {
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
        }
    }

    private fun parseCursor(cursor: String): Long =
        cursor.toLongOrNull()
            ?.takeIf { it > 0 }
            ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE)
}

data class UserNotificationResult(
    val notificationId: Long,
    val notificationType: NotificationType,
    val isbn: String,
    val bookTitle: String,
    val libraryId: Long,
    val libraryName: String,
    val title: String,
    val message: String,
    val notificationDate: LocalDate,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(notification: UserNotification): UserNotificationResult {
            val book = notification.book
            val library = notification.library
            return UserNotificationResult(
                notificationId = notification.id ?: throw ServiceException(ErrorCode.NOTIFICATION_NOT_FOUND),
                notificationType = notification.notificationType,
                isbn = book.isbn ?: throw ServiceException(ErrorCode.BOOK_NOT_FOUND),
                bookTitle = book.title,
                libraryId = library.id ?: throw ServiceException(ErrorCode.LIBRARY_NOT_FOUND),
                libraryName = library.name,
                title = notification.title,
                message = notification.message,
                notificationDate = notification.notificationDate,
                createdAt = notification.createdAt
            )
        }
    }
}
