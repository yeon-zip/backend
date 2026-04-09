package kr.ac.kumoh.polaris.library.implement

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryCursor
import kr.ac.kumoh.polaris.library.implement.dto.NearbyLibraryQueryResult
import kr.ac.kumoh.polaris.library.util.BoundingBox
import kr.ac.kumoh.polaris.library.util.BoundingBoxUtils
import org.springframework.stereotype.Repository

@Repository
class NearbyLibraryQueryRepository(
    private val entityManager: EntityManager
) {
    fun findWithinRadius(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<NearbyLibraryQueryResult> {
        val boundingBox = BoundingBoxUtils.calculate(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm
        )

        val query = entityManager.createNativeQuery(FIND_WITHIN_RADIUS_QUERY)
        bindCommonParameters(
            query = query,
            latitude = latitude,
            longitude = longitude,
            boundingBox = boundingBox,
            radiusKm = radiusKm
        )

        @Suppress("UNCHECKED_CAST")
        return (query.resultList as List<Array<Any?>>).map(::mapRow)
    }

    fun findPageWithinRadius(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        cursor: NearbyLibraryCursor?,
        limit: Int
    ): List<NearbyLibraryQueryResult> {
        val boundingBox = BoundingBoxUtils.calculate(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm
        )

        val queryString = if (cursor == null) {
            FIRST_PAGE_QUERY
        } else {
            NEXT_PAGE_QUERY
        }

        val query = entityManager.createNativeQuery(queryString)
        bindCommonParameters(
            query = query,
            latitude = latitude,
            longitude = longitude,
            boundingBox = boundingBox,
            radiusKm = radiusKm
        )
        query.setParameter("limit", limit)

        if (cursor != null) {
            query.setParameter("cursorDistanceMeter", cursor.distanceMeter)
            query.setParameter("cursorLibraryId", cursor.libraryId)
        }

        @Suppress("UNCHECKED_CAST")
        return (query.resultList as List<Array<Any?>>).map(::mapRow)
    }

    private fun bindCommonParameters(
        query: Query,
        latitude: Double,
        longitude: Double,
        boundingBox: BoundingBox,
        radiusKm: Double
    ) {
        query.setParameter("latitude", latitude)
        query.setParameter("longitude", longitude)
        query.setParameter("minLatitude", boundingBox.minLatitude)
        query.setParameter("maxLatitude", boundingBox.maxLatitude)
        query.setParameter("minLongitude", boundingBox.minLongitude)
        query.setParameter("maxLongitude", boundingBox.maxLongitude)
        query.setParameter("radiusMeter", radiusKm * 1000.0)
    }

    private fun mapRow(row: Array<Any?>): NearbyLibraryQueryResult {
        // 0: library_id
        // 1: lib_code
        // 2: name
        // 3: address
        // 4: latitude
        // 5: longitude
        // 6: homepage_url
        // 7: tel
        // 8: distance_km
        // 9: distance_meter
        return NearbyLibraryQueryResult(
            libraryId = row[0].toLongValue(),
            libCode = row[1] as String,
            name = row[2] as String,
            address = row[3] as? String ?: "",
            latitude = row[4].toDoubleValue(),
            longitude = row[5].toDoubleValue(),
            homepageUrl = row[6] as? String,
            tel = row[7] as? String,
            distanceKm = row[8].toDoubleValue(),
            distanceMeter = row[9].toLongValue()
        )
    }

    private fun Any?.toLongValue(): Long = (this as Number).toLong()

    private fun Any?.toDoubleValue(): Double = (this as Number).toDouble()

    companion object {
        private val BASE_SELECT_QUERY =
            """
            SELECT
                nearby.library_id,
                nearby.lib_code,
                nearby.name,
                nearby.address,
                nearby.latitude,
                nearby.longitude,
                nearby.homepage_url,
                nearby.tel,
                nearby.distance_meter / 1000.0 AS distance_km,
                nearby.distance_meter
            FROM (
                SELECT
                    base.library_id,
                    base.lib_code,
                    base.name,
                    base.address,
                    base.latitude,
                    base.longitude,
                    base.homepage_url,
                    base.tel,
                    CAST(ROUND(base.raw_distance_meter, 0) AS UNSIGNED) AS distance_meter,
                    base.raw_distance_meter
                FROM (
                    SELECT
                        l.id AS library_id,
                        l.lib_code AS lib_code,
                        l.name AS name,
                        CONCAT_WS(
                            ' ',
                            NULLIF(TRIM(l.address_province), ''),
                            NULLIF(TRIM(l.address_city), ''),
                            NULLIF(TRIM(l.address_detail), '')
                        ) AS address,
                        l.latitude AS latitude,
                        l.longitude AS longitude,
                        l.homepage_url AS homepage_url,
                        l.tel AS tel,
                        ST_DISTANCE_SPHERE(
                            POINT(l.longitude, l.latitude),
                            POINT(:longitude, :latitude)
                        ) AS raw_distance_meter
                    FROM library l
                    WHERE l.latitude IS NOT NULL
                      AND l.longitude IS NOT NULL
                      AND l.latitude BETWEEN :minLatitude AND :maxLatitude
                      AND l.longitude BETWEEN :minLongitude AND :maxLongitude
                ) base
            ) nearby
            WHERE nearby.raw_distance_meter <= :radiusMeter
            """.trimIndent()

        private const val ORDER_BY_AND_LIMIT =
            """
            ORDER BY nearby.distance_meter ASC, nearby.library_id ASC
            LIMIT :limit
            """

        private val KEYSET_WHERE_CLAUSE =
            """
            AND (
                nearby.distance_meter > :cursorDistanceMeter
                OR (
                    nearby.distance_meter = :cursorDistanceMeter
                    AND nearby.library_id > :cursorLibraryId
                )
            )
            """.trimIndent()

        private val FIND_WITHIN_RADIUS_QUERY =
            """
            $BASE_SELECT_QUERY
            ORDER BY nearby.distance_meter ASC, nearby.library_id ASC
            """.trimIndent()

        private val FIRST_PAGE_QUERY =
            """
            $BASE_SELECT_QUERY
            $ORDER_BY_AND_LIMIT
            """.trimIndent()

        private val NEXT_PAGE_QUERY =
            """
            $BASE_SELECT_QUERY
            $KEYSET_WHERE_CLAUSE
            $ORDER_BY_AND_LIMIT
            """.trimIndent()
    }
}
