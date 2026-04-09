package kr.ac.kumoh.polaris.book.implement

import kr.ac.kumoh.polaris.book.entity.Book
import kr.ac.kumoh.polaris.book.repository.BookRepository
import org.springframework.stereotype.Component

@Component
class BookWriter(
    private val bookRepository: BookRepository
) {
    fun saveIfAbsent(metadata: BookMetadata): Book =
        bookRepository.findByIsbn(metadata.isbn)
            ?: bookRepository.save(
                Book(
                    isbn = metadata.isbn,
                    title = metadata.title,
                    author = metadata.author,
                    publisher = metadata.publisher,
                    description = metadata.description,
                    publicationDate = metadata.publicationDate,
                    coverImageUrl = metadata.coverImageUrl
                )
            )
}
