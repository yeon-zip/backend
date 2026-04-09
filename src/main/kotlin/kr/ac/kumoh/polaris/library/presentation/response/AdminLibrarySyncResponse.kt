package kr.ac.kumoh.polaris.library.presentation.response

import java.time.LocalDateTime

data class AdminLibrarySyncResponse(
    val syncedCount: Int,
    val syncedAt: LocalDateTime
)
