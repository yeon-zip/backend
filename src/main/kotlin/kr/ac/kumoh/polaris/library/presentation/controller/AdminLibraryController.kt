package kr.ac.kumoh.polaris.library.presentation.controller

import io.swagger.v3.oas.annotations.Hidden
import kr.ac.kumoh.polaris.library.presentation.response.AdminLibrarySyncResponse
import kr.ac.kumoh.polaris.library.service.LibrarySyncService
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Hidden
@Profile("local")
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
