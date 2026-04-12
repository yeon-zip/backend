package kr.ac.kumoh.polaris.book.implement.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverBookDetailSearchResponse(
    val lastBuildDate: String? = null,
    val total: Int = 0,
    val start: Int = 1,
    val display: Int = 10,
    val items: List<NaverBookDetailItem> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverBookDetailItem(
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
