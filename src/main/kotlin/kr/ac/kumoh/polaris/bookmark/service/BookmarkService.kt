package kr.ac.kumoh.polaris.bookmark.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.bookmark.entity.BookBookmark
import kr.ac.kumoh.polaris.bookmark.entity.LibraryBookmark
import kr.ac.kumoh.polaris.bookmark.repository.BookBookmarkRepository
import kr.ac.kumoh.polaris.bookmark.repository.LibraryBookmarkRepository
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.global.util.IsbnNormalizer
import kr.ac.kumoh.polaris.library.implement.LibraryReader
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.implement.UserReader
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookmarkService(
    private val userReader: UserReader,
    private val libraryReader: LibraryReader,
    private val bookReader: BookReader,
    private val bookMetadataLoader: BookMetadataLoader,
    private val bookWriter: BookWriter,
    private val libraryBookmarkRepository: LibraryBookmarkRepository,
    private val bookBookmarkRepository: BookBookmarkRepository
) {
    @Transactional
    fun bookmarkLibrary(userId: Long, libraryId: Long) {
        val user = userReader.findByIdOrThrow(userId)
        val library = libraryReader.findByIdOrThrow(libraryId)
        val persistedLibraryId = library.id ?: throw ServiceException(ErrorCode.LIBRARY_NOT_FOUND)

        if (libraryBookmarkRepository.existsByUserIdAndLibraryId(userId, persistedLibraryId)) {
            throw ServiceException(
                errorCode = ErrorCode.BOOKMARK_ALREADY_EXISTS,
                message = "이미 찜한 도서관입니다. libraryId=$libraryId"
            )
        }

        try {
            libraryBookmarkRepository.saveAndFlush(
                LibraryBookmark(
                    user = user,
                    library = library
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw duplicateLibraryBookmarkException(libraryId)
        }
    }

    @Transactional
    fun unbookmarkLibrary(userId: Long, libraryId: Long) {
        userReader.findByIdOrThrow(userId)
        val library = libraryReader.findByIdOrThrow(libraryId)
        val persistedLibraryId = library.id ?: throw ServiceException(ErrorCode.LIBRARY_NOT_FOUND)

        libraryBookmarkRepository.findByUserIdAndLibraryId(userId, persistedLibraryId)
            ?.let(libraryBookmarkRepository::delete)
    }

    @Transactional(readOnly = true)
    fun getBookmarkedLibraries(userId: Long): List<BookmarkedLibraryResult> {
        userReader.findByIdOrThrow(userId)

        return libraryBookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .map { bookmark ->
                val library = bookmark.library
                BookmarkedLibraryResult(
                    libraryId = library.id ?: throw ServiceException(ErrorCode.LIBRARY_NOT_FOUND),
                    name = library.name,
                    address = library.address.toString()
                )
            }
    }

    @Transactional
    fun bookmarkBook(userId: Long, isbn: String) {
        val user = userReader.findByIdOrThrow(userId)
        val book = resolveBookByIsbnOrThrow(isbn)
        val bookId = book.id ?: throw ServiceException(ErrorCode.BOOK_NOT_FOUND)

        if (bookBookmarkRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw ServiceException(
                errorCode = ErrorCode.BOOKMARK_ALREADY_EXISTS,
                message = "이미 찜한 도서입니다. isbn=${book.isbn ?: isbn}"
            )
        }

        try {
            bookBookmarkRepository.saveAndFlush(
                BookBookmark(
                    user = user,
                    book = book
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw duplicateBookBookmarkException(book.isbn ?: isbn)
        }
    }

    @Transactional
    fun unbookmarkBook(userId: Long, isbn: String) {
        userReader.findByIdOrThrow(userId)

        val normalizedIsbn = IsbnNormalizer.normalize(isbn)
        val book = bookReader.findByIsbn(normalizedIsbn) ?: return
        val bookId = book.id ?: throw ServiceException(ErrorCode.BOOK_NOT_FOUND)

        bookBookmarkRepository.findByUserIdAndBookId(userId, bookId)
            ?.let(bookBookmarkRepository::delete)
    }

    @Transactional(readOnly = true)
    fun getBookmarkedBooks(userId: Long): List<BookmarkedBookResult> {
        userReader.findByIdOrThrow(userId)

        return bookBookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .map { bookmark ->
                val book = bookmark.book
                BookmarkedBookResult(
                    isbn = book.isbn ?: throw ServiceException(ErrorCode.BOOK_NOT_FOUND),
                    title = book.title,
                    author = book.author,
                    coverImageUrl = book.coverImageUrl
                )
            }
    }

    private fun resolveBookByIsbnOrThrow(isbn: String): Book {
        val normalizedIsbn = IsbnNormalizer.normalize(isbn)

        return bookReader.findByIsbn(normalizedIsbn)
            ?: bookMetadataLoader.loadByIsbn(normalizedIsbn)
                ?.let(bookWriter::saveIfAbsent)
            ?: throw ServiceException(
                errorCode = ErrorCode.BOOK_NOT_FOUND,
                message = "도서를 찾을 수 없습니다. isbn=$normalizedIsbn"
            )
    }

    private fun duplicateLibraryBookmarkException(libraryId: Long): ServiceException =
        ServiceException(
            errorCode = ErrorCode.BOOKMARK_ALREADY_EXISTS,
            message = "이미 찜한 도서관입니다. libraryId=$libraryId"
        )

    private fun duplicateBookBookmarkException(isbn: String): ServiceException =
        ServiceException(
            errorCode = ErrorCode.BOOKMARK_ALREADY_EXISTS,
            message = "이미 찜한 도서입니다. isbn=$isbn"
        )
}

data class BookmarkedLibraryResult(
    val libraryId: Long,
    val name: String,
    val address: String
)

data class BookmarkedBookResult(
    val isbn: String,
    val title: String,
    val author: String?,
    val coverImageUrl: String?
)
