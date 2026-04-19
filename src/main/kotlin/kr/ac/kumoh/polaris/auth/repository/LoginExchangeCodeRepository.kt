package kr.ac.kumoh.polaris.auth.repository

import jakarta.persistence.LockModeType
import kr.ac.kumoh.polaris.auth.entity.LoginExchangeCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface LoginExchangeCodeRepository : JpaRepository<LoginExchangeCode, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByCodeHash(codeHash: String): LoginExchangeCode?
}
