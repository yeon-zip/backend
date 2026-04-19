package kr.ac.kumoh.polaris.auth.repository

import jakarta.persistence.LockModeType
import kr.ac.kumoh.polaris.auth.entity.LoginExchangeCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LoginExchangeCodeRepository : JpaRepository<LoginExchangeCode, Long> {
    fun findByCodeHash(codeHash: String): LoginExchangeCode?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select exchangeCode
        from LoginExchangeCode exchangeCode
        join fetch exchangeCode.user
        where exchangeCode.codeHash = :codeHash
        """
    )
    fun findByCodeHashForUpdate(@Param("codeHash") codeHash: String): LoginExchangeCode?
}
