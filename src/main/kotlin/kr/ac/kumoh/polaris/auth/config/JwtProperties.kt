package kr.ac.kumoh.polaris.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "core.auth.jwt")
data class JwtProperties(
    val issuer: String,
    val secret: String,
    val accessTokenExpirationSeconds: Long,
    val refreshTokenExpirationSeconds: Long
)
