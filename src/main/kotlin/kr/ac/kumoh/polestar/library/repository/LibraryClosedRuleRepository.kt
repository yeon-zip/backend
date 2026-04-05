package kr.ac.kumoh.polestar.library.repository

import kr.ac.kumoh.polestar.library.entity.LibraryClosedRule
import org.springframework.data.jpa.repository.JpaRepository

interface LibraryClosedRuleRepository : JpaRepository<LibraryClosedRule, Long> {
    fun findAllByLibraryId(libraryId: Long): List<LibraryClosedRule>
}
