package kr.ac.kumoh.polaris.library.implement.client.dto


data class Data4LibraryLibrary(
    val libCode: String,
    val libName: String,
    val address: String?,
    val region: String?,
    val detailRegion: String?,
    val latitude: Double?,
    val longitude: Double?,
    val homepage: String?,
    val tel: String?,
    val fax: String?,
    val operatingTime: String?,
    val closed: String?
)
