package kr.ac.kumoh.polaris.library.repository

import kr.ac.kumoh.polaris.library.entity.Library
import org.springframework.data.jpa.repository.JpaRepository

interface LibraryRepository : JpaRepository<Library, Long> {
    fun findByLibCode(libCode: String): Library?
}
