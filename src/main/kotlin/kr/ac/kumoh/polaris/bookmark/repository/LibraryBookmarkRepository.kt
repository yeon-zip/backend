package kr.ac.kumoh.polaris.bookmark.repository

import kr.ac.kumoh.polaris.bookmark.entity.LibraryBookmark
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LibraryBookmarkRepository : JpaRepository<LibraryBookmark, Long> {
    fun existsByUserIdAndLibraryId(userId: Long, libraryId: Long): Boolean

    fun findByUserIdAndLibraryId(userId: Long, libraryId: Long): LibraryBookmark?

    @Query(
        """
        select libraryBookmark.library.id
        from LibraryBookmark libraryBookmark
        where libraryBookmark.user.id = :userId
          and libraryBookmark.library.id in :libraryIds
        """
    )
    fun findBookmarkedLibraryIdsByUserIdAndLibraryIdIn(
        @Param("userId") userId: Long,
        @Param("libraryIds") libraryIds: Collection<Long>
    ): List<Long>

    @Query(
        """
        select libraryBookmark
        from LibraryBookmark libraryBookmark
        join fetch libraryBookmark.library
        where libraryBookmark.user.id = :userId
        order by libraryBookmark.createdAt desc, libraryBookmark.id desc
        """
    )
    fun findAllByUserIdOrderByCreatedAtDesc(@Param("userId") userId: Long): List<LibraryBookmark>
}
