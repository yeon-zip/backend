package kr.ac.kumoh.polaris.auth.repository

import jakarta.persistence.LockModeType
import kr.ac.kumoh.polaris.auth.entity.AppExchangeCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AppExchangeCodeRepository : JpaRepository<AppExchangeCode, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select exchangeCode
        from AppExchangeCode exchangeCode
        join fetch exchangeCode.user
        where exchangeCode.codeHash = :codeHash
    """)
    fun findByCodeHashForUpdate(@Param("codeHash") codeHash: String): AppExchangeCode?

    fun findByCodeHash(codeHash: String): AppExchangeCode?
}
