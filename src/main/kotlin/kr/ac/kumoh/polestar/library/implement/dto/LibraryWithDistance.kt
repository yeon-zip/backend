package kr.ac.kumoh.polestar.library.implement.dto

import kr.ac.kumoh.polestar.library.entity.Library

data class LibraryWithDistance(
    val library: Library,
    val distanceKm: Double
)
