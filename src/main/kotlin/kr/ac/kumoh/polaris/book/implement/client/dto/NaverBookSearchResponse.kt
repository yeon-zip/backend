package kr.ac.kumoh.polaris.book.implement.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverBookSearchResponse(
    val lastBuildDate: String? = null,
    val total: Int = 0,
    val start: Int = 1,
    val display: Int = 10,
    val items: List<NaverBookSearchItem> = emptyList()
)
