package kr.ac.kumoh.polaris.library.implement.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Data4LibraryLibraryResponseItem(
    val libCode: String = "",
    val libName: String = "",
    val address: String? = null,
    val tel: String? = null,
    val fax: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val homepage: String? = null,
    val closed: String? = null,
    val operatingTime: String? = null
)
