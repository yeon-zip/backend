package kr.ac.kumoh.polestar.book.implement

import kr.ac.kumoh.polestar.book.entity.Book
import kr.ac.kumoh.polestar.book.repository.BookRepository
import org.springframework.stereotype.Component

@Component
class BookReader(
    private val bookRepository: BookRepository
) {
    fun findByIsbn(isbn: String): Book? =
        bookRepository.findByIsbn(isbn)
}
