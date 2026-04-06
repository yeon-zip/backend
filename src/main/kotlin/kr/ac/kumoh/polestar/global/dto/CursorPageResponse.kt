package kr.ac.kumoh.polestar.global.dto

data class CursorPageResponse<T>(
    val nextCursor: String?,
    val hasNext: Boolean,
    val items: List<T>
) {
    companion object {
        fun <T, R> from(
            result: CursorPageResult<T>,
            itemMapper: (T) -> R
        ): CursorPageResponse<R> =
            CursorPageResponse(
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
                items = result.items.map(itemMapper)
            )
    }
}
