package kr.ac.kumoh.polaris.bookavailability.service

import kr.ac.kumoh.polaris.bookavailability.implement.LibraryBookAvailabilityChecker
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookHoldingResult
import kr.ac.kumoh.polaris.global.util.IsbnNormalizer
import kr.ac.kumoh.polaris.library.implement.LibraryOpenStatusResolver
import kr.ac.kumoh.polaris.library.implement.LibraryReader
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class LibraryBookHoldingService(
    private val libraryReader: LibraryReader,
    private val libraryBookAvailabilityChecker: LibraryBookAvailabilityChecker,
    private val libraryOpenStatusResolver: LibraryOpenStatusResolver
) {
    fun getLibraryBookHolding(
        libraryId: Long,
        isbn: String
    ): LibraryBookHoldingResult {
        val normalizedIsbn = IsbnNormalizer.normalize(isbn)
        val library = libraryReader.findByIdOrThrow(libraryId)
        val availability = libraryBookAvailabilityChecker.check(
            libCode = library.libCode,
            isbn = normalizedIsbn
        )

        return LibraryBookHoldingResult(
            libraryId = libraryId,
            isbn = normalizedIsbn,
            hasBook = availability.hasBook,
            loanAvailable = availability.loanAvailable,
            availabilityStatus = availability.status,
            openNow = libraryOpenStatusResolver.isOpenNow(
                libraryId = libraryId,
                now = LocalDateTime.now()
            )
        )
    }
}
