package kr.ac.kumoh.polestar.book.implement.dto

import java.time.LocalDate

data class BookResult(
    val isbn: String,
    val title: String,
    val author: String?,
    val publisher: String?,
    val description: String?,
    val publicationDate: LocalDate?,
    val coverImageUrl: String?
)
