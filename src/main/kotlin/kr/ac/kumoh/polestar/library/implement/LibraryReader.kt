package kr.ac.kumoh.polestar.library.implement

import kr.ac.kumoh.polestar.global.exception.ErrorCode
import kr.ac.kumoh.polestar.global.exception.ServiceException
import kr.ac.kumoh.polestar.library.entity.Library
import kr.ac.kumoh.polestar.library.repository.LibraryRepository
import org.springframework.stereotype.Component

@Component
class LibraryReader(
    private val libraryRepository: LibraryRepository
) {
    fun findAll(): List<Library> = libraryRepository.findAll()

    fun findByIdOrThrow(libraryId: Long): Library =
        libraryRepository.findById(libraryId)
            .orElseThrow {
                ServiceException(
                    errorCode = ErrorCode.LIBRARY_NOT_FOUND,
                    message = "도서관을 찾을 수 없습니다. libraryId=$libraryId"
                )
            }
}
