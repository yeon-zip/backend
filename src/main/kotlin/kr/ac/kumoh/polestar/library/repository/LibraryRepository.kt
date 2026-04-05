package kr.ac.kumoh.polestar.library.repository

import kr.ac.kumoh.polestar.library.entity.Library
import org.springframework.data.jpa.repository.JpaRepository

interface LibraryRepository : JpaRepository<Library, Long> {
    fun findByLibCode(libCode: String): Library?
}
