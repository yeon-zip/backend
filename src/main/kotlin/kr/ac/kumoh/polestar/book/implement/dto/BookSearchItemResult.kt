package kr.ac.kumoh.polestar.book.implement.dto

import java.time.LocalDate

data class BookSearchItemResult(
    val isbn: String,
    val title: String,
    val author: String?,
    val publisher: String?,
    val description: String?,
    val publicationDate: LocalDate?,
    val coverImageUrl: String?,
    val link: String?
)
