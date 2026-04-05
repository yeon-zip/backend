package kr.ac.kumoh.polestar.library.presentation.controller

import kr.ac.kumoh.polestar.library.presentation.response.AdminLibrarySyncResponse
import kr.ac.kumoh.polestar.library.service.LibrarySyncService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/admin/libraries")
class AdminLibraryController(
    private val librarySyncService: LibrarySyncService
) {
    @PostMapping("/sync")
    fun syncLibraries(): ResponseEntity<AdminLibrarySyncResponse> {
        val syncedCount = librarySyncService.syncAll()

        return ResponseEntity.ok(
            AdminLibrarySyncResponse(
                syncedCount = syncedCount,
                syncedAt = LocalDateTime.now()
            )
        )
    }
}
