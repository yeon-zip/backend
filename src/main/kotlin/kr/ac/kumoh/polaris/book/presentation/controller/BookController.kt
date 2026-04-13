package kr.ac.kumoh.polaris.book.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.ac.kumoh.polaris.book.presentation.response.BookResponse
import kr.ac.kumoh.polaris.book.presentation.response.BookSearchItemResponse
import kr.ac.kumoh.polaris.book.service.BookSearchService
import kr.ac.kumoh.polaris.book.service.BookService
import kr.ac.kumoh.polaris.global.dto.CursorPageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "도서")
@RestController
@RequestMapping("/api/v1/books")
class BookController(
    private val bookService: BookService,
    private val bookSearchService: BookSearchService
) {
    @Operation(
        summary = "도서 조회",
        description = """
            <p>쿼리 파라미터 <code>query</code>에 해당하는 검색어와 연관된 도서의 목록을 검색합니다.</p>
            <p>응답의 <code>nextCursor</code> 값을 요청 시 쿼리 파라미터 <code>cursor</code>로 지정하면 해당 아이템 이후의 목록을 조회합니다.</p>
            <p>* 도서 조회는 최대 60초 소요됩니다. 시간 초과 한도를 60초로 설정하세요. 비정상적으로 많은 요청을 보내면 HTTP 상태 코드 <code>429 Too Many Requests</code>가 응답될 수 있습니다.</p>
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공"),
        ]
    )
    @Parameters(
        value = [
            Parameter(name = "query", description = "조회하고 싶은 도서의 검색어입니다."),
            Parameter(name = "cursor", description = "특정 도서 아이템 이후의 도서 아이템을 조회할 때 사용합니다."),
            Parameter(name = "limit", description = "한 번에 응답 받을 도서의 개수이고 기본값은 10이고 최대값은 100입니다.")
        ]
    )
    @GetMapping("/search")
    fun searchBooks(
        @RequestParam query: String,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<CursorPageResponse<BookSearchItemResponse>> {
        val result = bookSearchService.searchBooks(
            query = query,
            cursor = cursor,
            limit = limit
        )

        return ResponseEntity.ok(
            CursorPageResponse.from(result, BookSearchItemResponse::from)
        )
    }

    @Operation(
        summary = "도서 상세 조회",
        description = """
            <p>해당하는 ISBN에 대한 도서의 정보를 조회합니다.</p>
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공"),
        ]
    )
    @GetMapping("/{isbn}")
    fun getBook(
        @PathVariable isbn: String
    ): ResponseEntity<BookResponse> {
        val result = bookService.getBook(isbn)

        return ResponseEntity.ok(BookResponse.from(result))
    }
}
