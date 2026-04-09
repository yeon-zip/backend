package kr.ac.kumoh.polaris.library.implement.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Data4LibraryLibraryWrapper(
    val lib: Data4LibraryLibraryResponseItem = Data4LibraryLibraryResponseItem()
)
