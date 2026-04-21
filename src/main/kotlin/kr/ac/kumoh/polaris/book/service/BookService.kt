package kr.ac.kumoh.polaris.book.service

import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.book.implement.dto.BookResult
import kr.ac.kumoh.polaris.bookmark.implement.BookmarkStatusReader
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.global.util.IsbnNormalizer
import org.springframework.stereotype.Service

@Service
class BookService(
    private val bookReader: BookReader,
    private val bookMetadataLoader: BookMetadataLoader,
    private val bookWriter: BookWriter,
    private val bookmarkStatusReader: BookmarkStatusReader
) {
    fun getBook(
        isbn: String,
        userId: Long? = null
    ): BookResult {
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
            coverImageUrl = book.coverImageUrl,
            isBookmarked = bookmarkStatusReader.isBookBookmarked(userId, book.id)
        )
    }
}
