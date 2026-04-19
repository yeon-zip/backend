package kr.ac.kumoh.polaris.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "core.auth.oidc")
data class AuthAppLoginProperties(
    val issuerUri: String,
    val exchangeCodeTtlSeconds: Long,
    val legacyJsonCallback: LegacyJsonCallback = LegacyJsonCallback(),
    val appExchange: AppExchange = AppExchange(),
    val targets: Map<String, String> = emptyMap()
) {
    data class LegacyJsonCallback(
        val enabled: Boolean = false
    )

    data class AppExchange(
        val allowWithoutProof: Boolean = false
    )
}
