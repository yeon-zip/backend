package kr.ac.kumoh.polestar.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "core.cors")
data class CorsProperties(
    val allowedOrigins: List<String>,
    val allowedMethods: List<String>,
    val allowedHeaders: List<String>,
    val exposedHeaders: List<String>,
    val allowCredentials: Boolean,
    val maxAge: Long
)
