package kr.ac.kumoh.polestar.library.presentation.response

import java.time.LocalDateTime

data class AdminLibrarySyncResponse(
    val syncedCount: Int,
    val syncedAt: LocalDateTime
)
