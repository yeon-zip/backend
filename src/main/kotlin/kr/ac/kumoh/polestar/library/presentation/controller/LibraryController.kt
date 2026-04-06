package kr.ac.kumoh.polestar.library.presentation.controller

import kr.ac.kumoh.polestar.global.dto.CursorPageResponse
import kr.ac.kumoh.polestar.library.presentation.response.LibraryResponse
import kr.ac.kumoh.polestar.library.presentation.response.NearbyLibraryItemResponse
import kr.ac.kumoh.polestar.library.service.LibraryInfoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/libraries")
class LibraryController(
    private val libraryInfoService: LibraryInfoService
) {
    @GetMapping("/nearby")
    fun getNearbyLibraries(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(defaultValue = "5.0") radiusKm: Double,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<CursorPageResponse<NearbyLibraryItemResponse>> {
        val result = libraryInfoService.getNearbyLibraries(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            cursor = cursor,
            limit = limit
        )

        return ResponseEntity.ok(
            CursorPageResponse.from(result, NearbyLibraryItemResponse::from)
        )
    }

    @GetMapping("/{libraryId}")
    fun getLibrary(
        @PathVariable libraryId: Long
    ): ResponseEntity<LibraryResponse> {
        val result = libraryInfoService.getLibraryDetail(libraryId)

        return ResponseEntity.ok(LibraryResponse.from(result))
    }
}
