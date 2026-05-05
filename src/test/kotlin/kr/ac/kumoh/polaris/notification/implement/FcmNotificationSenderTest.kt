package kr.ac.kumoh.polaris.notification.implement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FcmNotificationSenderTest {
    @Test
    fun `sender chunks tokens by 500`() {
        val client = RecordingFcmMulticastClient { tokens ->
            FcmMulticastResponse(tokens.map { FcmTokenSendResult(success = true) })
        }
        val sender = FcmNotificationSender(client)
        val tokens = (1..501).map { "token-$it" }

        val result = sender.sendBookAvailable(tokens, payload())

        assertEquals(listOf(500, 1), client.sentChunks.map { it.size })
        assertEquals(501, result.successes.size)
        assertFalse(result.totalFailure)
    }

    @Test
    fun `per-token invalid failure is reported as permanent without failing all tokens`() {
        val client = RecordingFcmMulticastClient { tokens ->
            FcmMulticastResponse(
                tokens.mapIndexed { index, _ ->
                    when (index) {
                        1 -> FcmTokenSendResult(success = false, failureType = FcmFailureType.PERMANENT)
                        2 -> FcmTokenSendResult(success = false, failureType = FcmFailureType.TRANSIENT)
                        else -> FcmTokenSendResult(success = true)
                    }
                }
            )
        }
        val sender = FcmNotificationSender(client)

        val result = sender.sendBookAvailable(listOf("ok", "invalid", "retry"), payload())

        assertEquals(setOf("ok"), result.successes)
        assertEquals(setOf("invalid"), result.permanentFailures)
        assertEquals(setOf("retry"), result.transientFailures)
        assertFalse(result.totalFailure)
    }

    @Test
    fun `total-call exception does not mark tokens permanent`() {
        val client = RecordingFcmMulticastClient { throw IllegalStateException("FCM unavailable") }
        val sender = FcmNotificationSender(client)

        val result = sender.sendBookAvailable(listOf("a", "b"), payload())

        assertTrue(result.totalFailure)
        assertEquals(emptySet<String>(), result.successes)
        assertEquals(emptySet<String>(), result.permanentFailures)
        assertEquals(setOf("a", "b"), result.transientFailures)
    }

    private fun payload() = BookAvailableNotificationPayload(
        isbn = "9791191111111",
        libraryId = 1L,
        subscriptionId = 2L
    )

    private class RecordingFcmMulticastClient(
        private val handler: (List<String>) -> FcmMulticastResponse
    ) : FcmMulticastClient {
        val sentChunks = mutableListOf<List<String>>()

        override fun sendMulticast(
            tokens: List<String>,
            title: String,
            body: String,
            data: Map<String, String>
        ): FcmMulticastResponse {
            sentChunks += tokens
            assertEquals("대출 가능 알림", title)
            assertEquals("구독한 도서가 해당 도서관에서 대출 가능해졌습니다.", body)
            assertEquals("BOOK_LIBRARY_AVAILABLE", data["type"])
            return handler(tokens)
        }
    }
}
