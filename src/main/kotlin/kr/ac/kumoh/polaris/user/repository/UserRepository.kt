package kr.ac.kumoh.polaris.user.repository

import kr.ac.kumoh.polaris.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByOidcIssuerAndOidcSubject(oidcIssuer: String, oidcSubject: String): User?
}
