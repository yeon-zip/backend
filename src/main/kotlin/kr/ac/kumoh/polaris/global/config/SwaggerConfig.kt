package kr.ac.kumoh.polaris.global.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.context.annotation.Configuration

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@OpenAPIDefinition(
    info = Info(
        title = "Polaris API",
        version = "0.0.1"
    ),
    servers = [
        Server(
            url = "https://api.k-polaris.life",
            description = "Polaris API 서버"
        )
    ]
)
class SwaggerConfig {
}
