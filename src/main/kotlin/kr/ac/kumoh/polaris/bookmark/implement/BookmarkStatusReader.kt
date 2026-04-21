package kr.ac.kumoh.polaris.bookmark.implement

import kr.ac.kumoh.polaris.bookmark.repository.BookBookmarkRepository
import kr.ac.kumoh.polaris.bookmark.repository.LibraryBookmarkRepository
import org.springframework.stereotype.Component

@Component
class BookmarkStatusReader(
    private val bookBookmarkRepository: BookBookmarkRepository,
    private val libraryBookmarkRepository: LibraryBookmarkRepository
) {
    fun isBookBookmarked(
        userId: Long?,
        bookId: Long?
    ): Boolean {
        if (userId == null || bookId == null) {
            return false
        }

        return bookBookmarkRepository.existsByUserIdAndBookId(userId, bookId)
    }

    fun isLibraryBookmarked(
        userId: Long?,
        libraryId: Long?
    ): Boolean {
        if (userId == null || libraryId == null) {
            return false
        }

        return libraryBookmarkRepository.existsByUserIdAndLibraryId(userId, libraryId)
    }

    fun getBookmarkedIsbns(
        userId: Long?,
        isbns: Collection<String>
    ): Set<String> {
        if (userId == null || isbns.isEmpty()) {
            return emptySet()
        }

        return bookBookmarkRepository.findBookmarkedIsbnsByUserIdAndIsbnIn(
            userId = userId,
            isbns = isbns
        ).toSet()
    }

    fun getBookmarkedLibraryIds(
        userId: Long?,
        libraryIds: Collection<Long>
    ): Set<Long> {
        if (userId == null || libraryIds.isEmpty()) {
            return emptySet()
        }

        return libraryBookmarkRepository.findBookmarkedLibraryIdsByUserIdAndLibraryIdIn(
            userId = userId,
            libraryIds = libraryIds
        ).toSet()
    }
}
