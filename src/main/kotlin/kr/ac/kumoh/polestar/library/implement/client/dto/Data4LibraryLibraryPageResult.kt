package kr.ac.kumoh.polestar.library.implement.client.dto

data class Data4LibraryLibraryPageResult(
    val pageNo: Int,
    val pageSize: Int,
    val totalCount: Int,
    val resultCount: Int,
    val libraries: List<Data4LibraryLibrary>
) {
    val hasNext: Boolean
        get() = pageNo * pageSize < totalCount

    companion object {
        fun empty(pageNo: Int) = Data4LibraryLibraryPageResult(
            pageNo = pageNo,
            pageSize = 0,
            totalCount = 0,
            resultCount = 0,
            libraries = emptyList()
        )
    }
}
