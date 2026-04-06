package kr.ac.kumoh.polestar.bookavailability.implement.dto

import kr.ac.kumoh.polestar.library.entity.Library

class NearbyLibraryBookAvailability(
    val library: Library,
    val distanceKm: Double,
    val hasBook: Boolean?,
    val loanAvailable: Boolean?,
    val status: LibraryBookAvailabilityStatus
)
