package kr.ac.kumoh.polaris.library.implement

import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.library.entity.Library
import kr.ac.kumoh.polaris.library.repository.LibraryRepository
import org.springframework.stereotype.Component

@Component
class LibraryReader(
    private val libraryRepository: LibraryRepository
) {
    /**
     * 도서관 조회
     */
    fun findByIdOrThrow(libraryId: Long): Library =
        libraryRepository.findById(libraryId)
            .orElseThrow {
                ServiceException(
                    errorCode = ErrorCode.LIBRARY_NOT_FOUND,
                    message = "도서관을 찾을 수 없습니다. libraryId=$libraryId"
                )
            }
}
