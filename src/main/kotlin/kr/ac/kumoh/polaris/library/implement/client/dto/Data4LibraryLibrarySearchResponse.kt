package kr.ac.kumoh.polaris.library.implement.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Data4LibraryLibrarySearchResponse(
    val response: Data4LibraryLibrarySearchResponseBody = Data4LibraryLibrarySearchResponseBody()
)
