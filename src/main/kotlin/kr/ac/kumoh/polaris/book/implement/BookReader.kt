package kr.ac.kumoh.polaris.book.implement

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.repository.BookRepository
import org.springframework.stereotype.Component

@Component
class BookReader(
    private val bookRepository: BookRepository
) {
    fun findByIsbn(isbn: String): Book? =
        bookRepository.findByIsbn(isbn)
}
