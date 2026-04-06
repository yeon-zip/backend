package kr.ac.kumoh.polestar.book.repository

import kr.ac.kumoh.polestar.book.entity.Book
import org.springframework.data.jpa.repository.JpaRepository

interface BookRepository : JpaRepository<Book, Long> {
    fun findByIsbn(isbn: String): Book?
}
