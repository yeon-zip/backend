package kr.ac.kumoh.polestar.global.dto

data class CursorPageResult<T>(
    val nextCursor: String?,
    val hasNext: Boolean,
    val items: List<T>
)
