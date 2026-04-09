package kr.ac.kumoh.polaris.library.util

import kotlin.math.abs
import kotlin.math.cos

object BoundingBoxUtils {
    private const val KM_PER_LATITUDE_DEGREE = 111.32
    private const val MIN_LATITUDE = -90.0
    private const val MAX_LATITUDE = 90.0
    private const val MIN_LONGITUDE = -180.0
    private const val MAX_LONGITUDE = 180.0
    private const val MIN_COS_LATITUDE = 1e-12

    fun calculate(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): BoundingBox {
        val latitudeDelta = radiusKm / KM_PER_LATITUDE_DEGREE
        val minLatitude = (latitude - latitudeDelta).coerceAtLeast(MIN_LATITUDE)
        val maxLatitude = (latitude + latitudeDelta).coerceAtMost(MAX_LATITUDE)

        val cosineLatitude = abs(cos(Math.toRadians(latitude)))
        val longitudeDelta = if (cosineLatitude < MIN_COS_LATITUDE) {
            Double.POSITIVE_INFINITY
        } else {
            radiusKm / (KM_PER_LATITUDE_DEGREE * cosineLatitude)
        }

        val rawMinLongitude = longitude - longitudeDelta
        val rawMaxLongitude = longitude + longitudeDelta
        val usesFullLongitudeRange =
            longitudeDelta.isInfinite() ||
                rawMinLongitude < MIN_LONGITUDE ||
                rawMaxLongitude > MAX_LONGITUDE

        val minLongitude = if (usesFullLongitudeRange) {
            MIN_LONGITUDE
        } else {
            rawMinLongitude.coerceAtLeast(MIN_LONGITUDE)
        }
        val maxLongitude = if (usesFullLongitudeRange) {
            MAX_LONGITUDE
        } else {
            rawMaxLongitude.coerceAtMost(MAX_LONGITUDE)
        }

        return BoundingBox(
            minLatitude = minLatitude,
            maxLatitude = maxLatitude,
            minLongitude = minLongitude,
            maxLongitude = maxLongitude
        )
    }
}

data class BoundingBox(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double
)
