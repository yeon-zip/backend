package kr.ac.kumoh.polestar.library.implement

import kr.ac.kumoh.polestar.library.entity.LibraryClosedRule
import kr.ac.kumoh.polestar.library.repository.LibraryClosedRuleRepository
import org.springframework.stereotype.Component

@Component
class LibraryClosedRuleReader(
    private val libraryClosedRuleRepository: LibraryClosedRuleRepository
) {
    fun findClosedRules(libraryId: Long): List<LibraryClosedRule> =
        libraryClosedRuleRepository.findAllByLibraryId(libraryId)
}
