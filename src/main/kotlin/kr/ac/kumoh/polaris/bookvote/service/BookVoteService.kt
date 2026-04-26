package kr.ac.kumoh.polaris.bookvote.service

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.implement.BookMetadataLoader
import kr.ac.kumoh.polaris.book.implement.BookReader
import kr.ac.kumoh.polaris.book.implement.BookWriter
import kr.ac.kumoh.polaris.bookvote.entity.BookVote
import kr.ac.kumoh.polaris.bookvote.entity.BookVoteType
import kr.ac.kumoh.polaris.bookvote.repository.BookVoteRepository
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.global.util.IsbnNormalizer
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.implement.UserReader
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookVoteService(
    private val userReader: UserReader,
    private val bookReader: BookReader,
    private val bookMetadataLoader: BookMetadataLoader,
    private val bookWriter: BookWriter,
    private val bookVoteRepository: BookVoteRepository
) {
    @Transactional
    fun voteBook(
        userId: Long,
        isbn: String,
        voteType: BookVoteType
    ) {
        val user = userReader.findByIdOrThrow(userId)
        val book = resolveBookByIsbnOrThrow(isbn)
        val bookId = book.id ?: throw ServiceException(ErrorCode.BOOK_NOT_FOUND)

        val existingVote = bookVoteRepository.findByUserIdAndBookId(userId, bookId)
        if (existingVote != null) {
            existingVote.updateVoteType(voteType)
            return
        }

        createVoteWithRaceFallback(
            userId = userId,
            bookId = bookId,
            user = user,
            book = book,
            voteType = voteType
        )
    }

    private fun createVoteWithRaceFallback(
        userId: Long,
        bookId: Long,
        user: User,
        book: Book,
        voteType: BookVoteType
    ) {
        try {
            bookVoteRepository.saveAndFlush(
                BookVote(
                    user = user,
                    book = book,
                    voteType = voteType
                )
            )
        } catch (exception: DataIntegrityViolationException) {
            val existingVote = bookVoteRepository.findByUserIdAndBookId(userId, bookId)
                ?: throw exception
            existingVote.updateVoteType(voteType)
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
}
