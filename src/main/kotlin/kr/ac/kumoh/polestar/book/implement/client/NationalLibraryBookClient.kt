package kr.ac.kumoh.polestar.book.implement.client

import kr.ac.kumoh.polestar.global.properties.NationalLibraryApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Component
class NationalLibraryBookClient(
    private val nationalLibraryRestClient: RestClient,
    private val properties: NationalLibraryApiProperties,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun fetchByIsbn(isbn: String): NationalLibraryBookDetail? {
        if (properties.apiKey.isBlank()) {
            log.warn("국립중앙도서관 인증키가 비어 있어 도서 metadata 를 조회할 수 없습니다. isbn={}", isbn)
            return null
        }

        val responseBody = try {
            nationalLibraryRestClient.get()
                .uri { builder ->
                    builder.path("/seoji/SearchApi.do")
                        .queryParam("cert_key", properties.apiKey)
                        .queryParam("result_style", "json")
                        .queryParam("page_no", 1)
                        .queryParam("page_size", 1)
                        .queryParam("isbn", isbn)
                        .build()
                }
                .retrieve()
                .body(String::class.java)
        } catch (e: Exception) {
            log.error("국립중앙도서관 도서 metadata 조회 실패 - isbn={}", isbn, e)
            return null
        }

        if (responseBody.isNullOrBlank()) {
            log.warn("국립중앙도서관 도서 metadata 응답이 비어 있습니다. isbn={}", isbn)
            return null
        }

        val root = try {
            objectMapper.readTree(responseBody)
        } catch (e: Exception) {
            log.error("국립중앙도서관 도서 metadata 응답 파싱 실패 - isbn={}", isbn, e)
            return null
        }

        if (root.path("RESULT").textValue().orEmpty().equals("ERROR", ignoreCase = true)) {
            log.warn(
                "국립중앙도서관 도서 metadata 조회 오류 - isbn={}, code={}, message={}",
                isbn,
                root.path("ERR_CODE").textValue().orEmpty(),
                root.path("ERR_MESSAGE").textValue().orEmpty()
            )
            return null
        }

        val detailNode = findFirstBookNode(root)
            ?: run {
                log.info("국립중앙도서관 조회 결과에서 도서 metadata 를 찾지 못했습니다. isbn={}", isbn)
                return null
            }

        val title = detailNode.fieldText("TITLE") ?: return null

        return NationalLibraryBookDetail(
            isbn = detailNode.fieldText("EA_ISBN")?.extractIsbn13() ?: isbn,
            title = title,
            author = detailNode.fieldText("AUTHOR"),
            publisher = detailNode.fieldText("PUBLISHER"),
            description = null,
            publicationDate = detailNode.fieldText("PUBLISH_PREDATE").toPublicationDateOrNull(),
            coverImageUrl = detailNode.fieldText("TITLE_URL")
        )
    }

    private fun findFirstBookNode(node: JsonNode): JsonNode? {
        if (node.isObject && (node.hasNonNull("TITLE") || node.hasNonNull("EA_ISBN"))) {
            return node
        }

        node.forEach { child ->
            val found = findFirstBookNode(child)
            if (found != null) {
                return found
            }
        }

        return null
    }

    private fun JsonNode.fieldText(fieldName: String): String? =
        path(fieldName)
            .textValue()
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    private fun String.extractIsbn13(): String? =
        Regex("\\d{13}").find(this)?.value

    private fun String?.toPublicationDateOrNull(): LocalDate? {
        val value = this?.trim()?.takeIf { it.matches(Regex("\\d{8}")) } ?: return null

        return try {
            LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

data class NationalLibraryBookDetail(
    val isbn: String,
    val title: String,
    val author: String?,
    val publisher: String?,
    val description: String?,
    val publicationDate: LocalDate?,
    val coverImageUrl: String?
)
