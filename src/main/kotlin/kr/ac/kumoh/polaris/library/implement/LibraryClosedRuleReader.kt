package kr.ac.kumoh.polaris.library.implement

import kr.ac.kumoh.polaris.library.entity.LibraryClosedRule
import kr.ac.kumoh.polaris.library.repository.LibraryClosedRuleRepository
import org.springframework.stereotype.Component

@Component
class LibraryClosedRuleReader(
    private val libraryClosedRuleRepository: LibraryClosedRuleRepository
) {
    fun findClosedRules(libraryId: Long): List<LibraryClosedRule> =
        libraryClosedRuleRepository.findAllByLibraryId(libraryId)

    fun findClosedRuleMap(libraryIds: Collection<Long>): Map<Long, List<LibraryClosedRule>> {
        if (libraryIds.isEmpty()) {
            return emptyMap()
        }

        return libraryClosedRuleRepository.findAllByLibraryIdIn(libraryIds.distinct())
            .groupBy { closedRule ->
                requireNotNull(closedRule.library.id) {
                    "휴관 규칙에 연결된 도서관 ID가 없습니다. ruleId=${closedRule.id}"
                }
            }
    }
}
