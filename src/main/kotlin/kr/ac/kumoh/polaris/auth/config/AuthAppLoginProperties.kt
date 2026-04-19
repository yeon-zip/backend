package kr.ac.kumoh.polaris.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "core.auth.oidc")
data class AuthAppLoginProperties(
    val app: App = App(),
    val legacyJsonCallback: LegacyJsonCallback = LegacyJsonCallback()
) {
    fun resolveTarget(targetId: String): String? = app.targets[targetId]

    data class App(
        val targets: Map<String, String> = emptyMap(),
        val exchange: Exchange = Exchange()
    )

    data class Exchange(
        val ttl: Duration = Duration.ofMinutes(3),
        val allowWithoutProof: Boolean = false
    )

    data class LegacyJsonCallback(
        val enabled: Boolean = false
    )
}
