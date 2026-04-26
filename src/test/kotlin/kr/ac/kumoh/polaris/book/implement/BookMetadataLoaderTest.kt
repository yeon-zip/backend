package kr.ac.kumoh.polaris.book.implement

import kr.ac.kumoh.polaris.book.implement.client.NaverSearchBookDetailClient
import kr.ac.kumoh.polaris.book.implement.client.dto.NaverBookDetailItem
import kr.ac.kumoh.polaris.book.implement.client.dto.NaverBookDetailSearchResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BookMetadataLoaderTest {
    @Test
    fun `load by isbn extracts isbn13 from raw external isbn`() {
        val client = mock(NaverSearchBookDetailClient::class.java)
        val loader = BookMetadataLoader(client)

        `when`(client.searchBookDetails(isbn = "9788936434120", display = 1)).thenReturn(
            NaverBookDetailSearchResponse(
                items = listOf(
                    NaverBookDetailItem(
                        title = "아몬드",
                        isbn = "8936434120 9788936434120",
                        author = "손원평"
                    )
                )
            )
        )

        val metadata = loader.loadByIsbn("9788936434120")

        assertEquals("9788936434120", metadata?.isbn)
    }

    @Test
    fun `load by isbn falls back to request isbn when external isbn13 is missing`() {
        val client = mock(NaverSearchBookDetailClient::class.java)
        val loader = BookMetadataLoader(client)

        `when`(client.searchBookDetails(isbn = "9788936434120", display = 1)).thenReturn(
            NaverBookDetailSearchResponse(
                items = listOf(
                    NaverBookDetailItem(
                        title = "아몬드",
                        isbn = "8936434120",
                        author = "손원평"
                    )
                )
            )
        )

        val metadata = loader.loadByIsbn("9788936434120")

        assertEquals("9788936434120", metadata?.isbn)
    }
}
