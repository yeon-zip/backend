package kr.ac.kumoh.polaris.notification.scheduler

import kr.ac.kumoh.polaris.notification.config.NotificationProperties
import kr.ac.kumoh.polaris.notification.service.NotificationDispatchService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

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
}
