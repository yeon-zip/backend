package kr.ac.kumoh.polaris.global.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GlobalExceptionHandlerTest {
    private val globalExceptionHandler = GlobalExceptionHandler()

    @Test
    fun `bookmark already exists service exception maps to 409 problem detail`() {
        val response = globalExceptionHandler.handleServiceException(
            ServiceException(
                errorCode = ErrorCode.BOOKMARK_ALREADY_EXISTS,
                message = "이미 찜한 도서입니다. isbn=9791198363510"
            )
        )

        assertEquals(409, response.statusCode.value())
        assertEquals("Conflict", response.body?.title)
        assertEquals("BOOKMARK_ALREADY_EXISTS", response.body?.properties?.get("errorCode"))
    }
}
