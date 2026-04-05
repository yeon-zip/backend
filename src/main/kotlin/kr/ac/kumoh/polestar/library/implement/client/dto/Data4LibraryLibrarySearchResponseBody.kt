package kr.ac.kumoh.polestar.library.implement.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Data4LibraryLibrarySearchResponseBody(
    val pageNo: Int = 0,
    val pageSize: Int = 0,
    val numFound: Int = 0,
    val resultNum: Int = 0,
    val libs: List<Data4LibraryLibraryWrapper> = emptyList()
)
