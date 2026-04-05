package kr.ac.kumoh.polestar.book.service

import kr.ac.kumoh.polestar.book.implement.BookMetadataLoader
import kr.ac.kumoh.polestar.book.implement.BookReader
import kr.ac.kumoh.polestar.book.implement.BookWriter
import kr.ac.kumoh.polestar.book.implement.dto.BookResult
import kr.ac.kumoh.polestar.global.exception.ErrorCode
import kr.ac.kumoh.polestar.global.exception.ServiceException
import kr.ac.kumoh.polestar.global.util.IsbnNormalizer
import org.springframework.stereotype.Service

@Service
class BookService(
    private val bookReader: BookReader,
    private val bookMetadataLoader: BookMetadataLoader,
    private val bookWriter: BookWriter
) {
    fun getBook(isbn: String): BookResult {
        val normalizedIsbn = IsbnNormalizer.normalize(isbn)
        val book = bookReader.findByIsbn(normalizedIsbn)
            ?: bookMetadataLoader.loadByIsbn(normalizedIsbn)
                ?.let(bookWriter::saveIfAbsent)
            ?: throw ServiceException(
                errorCode = ErrorCode.BOOK_NOT_FOUND,
                message = "도서를 찾을 수 없습니다. isbn=$normalizedIsbn"
            )

        return BookResult(
            isbn = book.isbn ?: normalizedIsbn,
            title = book.title,
            author = book.author,
            publisher = book.publisher,
            description = book.description,
            publicationDate = book.publicationDate,
            coverImageUrl = book.coverImageUrl
        )
    }
}
