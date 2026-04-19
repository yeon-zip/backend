package kr.ac.kumoh.polaris.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "core.auth.oidc.app-exchange")
data class AuthAppLoginProperties(
    val ttlSeconds: Long = 180,
    val allowWithoutProof: Boolean = false
)
