package kr.ac.kumoh.polaris.auth.repository

import kr.ac.kumoh.polaris.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun deleteByToken(token: String)
    fun deleteAllByUserId(userId: Long)
}
