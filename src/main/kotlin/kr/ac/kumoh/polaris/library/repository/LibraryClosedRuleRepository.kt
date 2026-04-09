package kr.ac.kumoh.polaris.library.repository

import kr.ac.kumoh.polaris.library.entity.LibraryClosedRule
import org.springframework.data.jpa.repository.JpaRepository

interface LibraryClosedRuleRepository : JpaRepository<LibraryClosedRule, Long> {
    fun findAllByLibraryId(libraryId: Long): List<LibraryClosedRule>
    fun findAllByLibraryIdIn(libraryIds: Collection<Long>): List<LibraryClosedRule>
}
