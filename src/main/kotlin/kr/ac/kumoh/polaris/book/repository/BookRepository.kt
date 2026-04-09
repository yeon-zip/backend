package kr.ac.kumoh.polaris.book.repository

import kr.ac.kumoh.polaris.book.entity.Book
import org.springframework.data.jpa.repository.JpaRepository

interface BookRepository : JpaRepository<Book, Long> {
    fun findByIsbn(isbn: String): Book?
}
