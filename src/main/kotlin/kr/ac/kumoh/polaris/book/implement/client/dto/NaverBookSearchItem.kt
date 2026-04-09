package kr.ac.kumoh.polaris.book.implement.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverBookSearchItem(
    val title: String? = null,
    val link: String? = null,
    val image: String? = null,
    val author: String? = null,
    val discount: String? = null,
    val publisher: String? = null,
    val isbn: String? = null,
    val description: String? = null,
    val pubdate: String? = null
)
