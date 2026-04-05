package kr.ac.kumoh.polestar.bookavailability.service

import kr.ac.kumoh.polestar.bookavailability.implement.LibraryBookAvailabilityChecker
import kr.ac.kumoh.polestar.bookavailability.implement.dto.LibraryBookHoldingResult
import kr.ac.kumoh.polestar.global.util.IsbnNormalizer
import kr.ac.kumoh.polestar.library.implement.LibraryReader
import kr.ac.kumoh.polestar.library.service.LibraryScheduleService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class LibraryBookHoldingService(
    private val libraryReader: LibraryReader,
    private val libraryBookAvailabilityChecker: LibraryBookAvailabilityChecker,
    private val libraryScheduleService: LibraryScheduleService
) {
    fun getLibraryBookHolding(
        libraryId: Long,
        isbn: String
    ): LibraryBookHoldingResult {
        val normalizedIsbn = IsbnNormalizer.normalize(isbn)
        val library = libraryReader.findByIdOrThrow(libraryId)
        val resolvedLibraryId = requireNotNull(library.id)
        val availability = libraryBookAvailabilityChecker.check(
            libCode = library.libCode,
            isbn = normalizedIsbn
        )

        return LibraryBookHoldingResult(
            libraryId = resolvedLibraryId,
            isbn = normalizedIsbn,
            hasBook = availability.hasBook,
            loanAvailable = availability.loanAvailable,
            availabilityStatus = availability.status,
            openNow = libraryScheduleService.isOpenNow(
                libraryId = resolvedLibraryId,
                now = LocalDateTime.now()
            )
        )
    }
}
