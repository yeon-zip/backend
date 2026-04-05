package kr.ac.kumoh.polestar.library.service

import kr.ac.kumoh.polestar.global.dto.CursorPageResult
import kr.ac.kumoh.polestar.global.exception.ErrorCode
import kr.ac.kumoh.polestar.global.exception.ServiceException
import kr.ac.kumoh.polestar.library.entity.LibraryClosedRule
import kr.ac.kumoh.polestar.library.entity.LibraryOperatingHour
import kr.ac.kumoh.polestar.library.implement.LibraryClosedRuleReader
import kr.ac.kumoh.polestar.library.implement.LibraryOperatingHourReader
import kr.ac.kumoh.polestar.library.implement.LibraryReader
import kr.ac.kumoh.polestar.library.implement.NearbyLibraryFinder
import kr.ac.kumoh.polestar.library.implement.dto.ClosedRuleResult
import kr.ac.kumoh.polestar.library.implement.dto.LibraryDetailResult
import kr.ac.kumoh.polestar.library.implement.dto.LibraryWithDistance
import kr.ac.kumoh.polestar.library.implement.dto.NearbyLibraryCursor
import kr.ac.kumoh.polestar.library.implement.dto.NearbyLibraryItemResult
import kr.ac.kumoh.polestar.library.implement.dto.TodayOperatingHourResult
import kr.ac.kumoh.polestar.library.implement.dto.WeeklyOperatingHourResult
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class LibraryInfoService(
    private val libraryReader: LibraryReader,
    private val nearbyLibraryFinder: NearbyLibraryFinder,
    private val libraryScheduleService: LibraryScheduleService,
    private val libraryOperatingHourReader: LibraryOperatingHourReader,
    private val libraryClosedRuleReader: LibraryClosedRuleReader
) {
    fun getLibraryDetail(libraryId: Long): LibraryDetailResult {
        val library = libraryReader.findByIdOrThrow(libraryId)
        val resolvedLibraryId = requireNotNull(library.id)

        val now = LocalDateTime.now()
        val todayOperatingHour = libraryScheduleService.getTodayOperatingHour(
            libraryId = resolvedLibraryId,
            date = now.toLocalDate()
        )
        val weeklyOperatingHours = libraryOperatingHourReader.findWeeklyOperatingHours(resolvedLibraryId)
            .sortedBy { it.weekday }
        val closedRules = libraryClosedRuleReader.findClosedRules(resolvedLibraryId)

        return LibraryDetailResult(
            libraryId = resolvedLibraryId,
            name = library.name,
            address = library.address.toString(),
            latitude = library.location.latitude,
            longitude = library.location.longitude,
            homepageUrl = library.contactInfo.homepageUrl,
            tel = library.contactInfo.tel,
            openNow = libraryScheduleService.isOpenNow(
                libraryId = resolvedLibraryId,
                now = now
            ),
            todayOperatingHour = todayOperatingHour?.toTodayResult(),
            weeklyOperatingHours = weeklyOperatingHours.map { it.toWeeklyResult() },
            closedRules = closedRules.map { it.toClosedRuleResult() }
        )
    }

    fun getNearbyLibraries(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        cursor: String?,
        limit: Int
    ): CursorPageResult<NearbyLibraryItemResult> {
        if (radiusKm <= 0) {
            throw ServiceException(ErrorCode.INVALID_RADIUS_KM)
        }
        if (limit !in 1..100) {
            throw ServiceException(ErrorCode.INVALID_LIMIT)
        }

        val parsedCursor = cursor?.let(::parseCursor)
        val nearbyLibraries = nearbyLibraryFinder.findWithinRadius(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm
        )
        val sortedLibraries = sortNearbyLibraries(nearbyLibraries)
        val filteredLibraries = applyCursor(sortedLibraries, parsedCursor)
        val pageItems = filteredLibraries.take(limit)
        val hasNext = filteredLibraries.size > limit
        val nextCursor = if (hasNext) pageItems.lastOrNull()?.toCursor() else null
        val openNowStatuses = getOpenNowStatuses(
            libraryIds = pageItems.map { libraryWithDistance ->
                requireNotNull(libraryWithDistance.library.id)
            },
            now = LocalDateTime.now()
        )

        return CursorPageResult(
            nextCursor = nextCursor,
            hasNext = hasNext,
            items = pageItems.map { libraryWithDistance ->
                val libraryId = requireNotNull(libraryWithDistance.library.id)
                NearbyLibraryItemResult(
                    libraryId = libraryId,
                    name = libraryWithDistance.library.name,
                    address = libraryWithDistance.library.address.toString(),
                    latitude = libraryWithDistance.library.location.latitude,
                    longitude = libraryWithDistance.library.location.longitude,
                    homepageUrl = libraryWithDistance.library.contactInfo.homepageUrl,
                    tel = libraryWithDistance.library.contactInfo.tel,
                    distanceKm = libraryWithDistance.distanceKm,
                    openNow = findOpenNow(
                        libraryId = libraryId,
                        openNowStatuses = openNowStatuses
                    )
                )
            }
        )
    }

    fun getOpenNowStatuses(
        libraryIds: Collection<Long>,
        now: LocalDateTime
    ): List<LibraryOpenNowStatus> =
        libraryScheduleService.getOpenNowStatuses(
            libraryIds = libraryIds,
            now = now
        )

    private fun sortNearbyLibraries(
        nearbyLibraries: List<LibraryWithDistance>
    ): List<LibraryWithDistance> =
        nearbyLibraries.sortedWith(
            compareBy<LibraryWithDistance> { it.distanceKm }
                .thenBy { requireNotNull(it.library.id) }
        )

    private fun applyCursor(
        nearbyLibraries: List<LibraryWithDistance>,
        cursor: NearbyLibraryCursor?
    ): List<LibraryWithDistance> {
        if (cursor == null) {
            return nearbyLibraries
        }

        return nearbyLibraries.filter { libraryWithDistance ->
            val libraryId = requireNotNull(libraryWithDistance.library.id)
            libraryWithDistance.distanceKm > cursor.distanceKm ||
                (libraryWithDistance.distanceKm == cursor.distanceKm && libraryId > cursor.libraryId)
        }
    }

    private fun parseCursor(rawCursor: String): NearbyLibraryCursor {
        val parts = rawCursor.split(":")
        if (parts.size != 2) {
            throw ServiceException(ErrorCode.INVALID_CURSOR)
        }

        val distanceKm = parts[0].toDoubleOrNull()
            ?: throw ServiceException(ErrorCode.INVALID_CURSOR)
        val libraryId = parts[1].toLongOrNull()
            ?: throw ServiceException(ErrorCode.INVALID_CURSOR)

        return NearbyLibraryCursor(
            distanceKm = distanceKm,
            libraryId = libraryId
        )
    }

    private fun LibraryWithDistance.toCursor(): String {
        val libraryId = requireNotNull(library.id)
        return "$distanceKm:$libraryId"
    }

    private fun findOpenNow(
        libraryId: Long,
        openNowStatuses: List<LibraryOpenNowStatus>
    ): Boolean =
        openNowStatuses.firstOrNull { status -> status.libraryId == libraryId }?.openNow ?: false

    private fun LibraryOperatingHour.toTodayResult(): TodayOperatingHourResult =
        TodayOperatingHourResult(
            weekday = weekday,
            openTime = openTime,
            closeTime = closeTime,
            isClosed = isClosed
        )

    private fun LibraryOperatingHour.toWeeklyResult(): WeeklyOperatingHourResult =
        WeeklyOperatingHourResult(
            weekday = weekday,
            openTime = openTime,
            closeTime = closeTime,
            isClosed = isClosed
        )

    private fun LibraryClosedRule.toClosedRuleResult(): ClosedRuleResult =
        ClosedRuleResult(
            ruleType = ruleType.name,
            weekday = weekday,
            nthWeek = nthWeek,
            monthDay = monthDay
        )
}
