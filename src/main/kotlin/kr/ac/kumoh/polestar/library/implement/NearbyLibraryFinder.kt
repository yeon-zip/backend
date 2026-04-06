package kr.ac.kumoh.polestar.library.implement

import kr.ac.kumoh.polestar.library.implement.dto.LibraryWithDistance
import kr.ac.kumoh.polestar.library.util.DistanceUtils
import org.springframework.stereotype.Component

@Component
class NearbyLibraryFinder(
    private val libraryReader: LibraryReader
) {
    fun findWithinRadius(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<LibraryWithDistance> =
        libraryReader.findAll()
            .asSequence()
            .mapNotNull { library ->
                val libraryLatitude = library.location.latitude ?: return@mapNotNull null
                val libraryLongitude = library.location.longitude ?: return@mapNotNull null

                val distanceKm = DistanceUtils.calculateKm(
                    lat1 = latitude,
                    lon1 = longitude,
                    lat2 = libraryLatitude,
                    lon2 = libraryLongitude
                )

                LibraryWithDistance(
                    library = library,
                    distanceKm = distanceKm
                )
            }
            .filter { it.distanceKm <= radiusKm }
            .sortedBy { it.distanceKm }
            .toList()
}
