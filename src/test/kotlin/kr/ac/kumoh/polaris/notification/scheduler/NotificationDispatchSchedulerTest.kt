package kr.ac.kumoh.polaris.notification.scheduler

import kr.ac.kumoh.polaris.notification.config.NotificationProperties
import kr.ac.kumoh.polaris.notification.service.NotificationDispatchResult
import kr.ac.kumoh.polaris.notification.service.NotificationDispatchService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NotificationDispatchSchedulerTest {
    @Test
    fun `scheduler default disabled does not invoke dispatch service`() {
        val dispatchService = mock(NotificationDispatchService::class.java)
        val scheduler = NotificationDispatchScheduler(
            notificationProperties = NotificationProperties(),
            notificationDispatchService = dispatchService
        )

        scheduler.dispatch()

        verifyNoInteractions(dispatchService)
    }

    @Test
    fun `scheduler enabled invokes dispatch service`() {
        val dispatchService = mock(NotificationDispatchService::class.java)
        `when`(dispatchService.dispatchOnce(anyDateTime())).thenReturn(
            NotificationDispatchResult(0, 0, 0, 0)
        )
        val scheduler = NotificationDispatchScheduler(
            notificationProperties = enabledProperties(),
            notificationDispatchService = dispatchService
        )

        scheduler.dispatch()

        verify(dispatchService).dispatchOnce(anyDateTime())
    }

    @Test
    fun `scheduler skips overlapping execution`() {
        val dispatchService = mock(NotificationDispatchService::class.java)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        doAnswer {
            started.countDown()
            release.await(2, TimeUnit.SECONDS)
            NotificationDispatchResult(0, 0, 0, 0)
        }.`when`(dispatchService).dispatchOnce(anyDateTime())
        val scheduler = NotificationDispatchScheduler(
            notificationProperties = enabledProperties(),
            notificationDispatchService = dispatchService
        )
        val executor = Executors.newSingleThreadExecutor()

        try {
            executor.submit { scheduler.dispatch() }
            started.await(2, TimeUnit.SECONDS)
            scheduler.dispatch()
            release.countDown()
        } finally {
            release.countDown()
            executor.shutdownNow()
        }

        verify(dispatchService, times(1)).dispatchOnce(anyDateTime())
    }

    private fun anyDateTime(): LocalDateTime =
        any(LocalDateTime::class.java) ?: LocalDateTime.MIN

    private fun enabledProperties(): NotificationProperties =
        NotificationProperties(
            dispatch = NotificationProperties.Dispatch(enabled = true)
        )
}
