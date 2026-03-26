package kr.ac.kumoh.polestar.global.config

import kr.ac.kumoh.polestar.global.handler.CustomAccessDeniedHandler
import kr.ac.kumoh.polestar.global.handler.CustomAuthenticationEntryPointHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val objectMapper: ObjectMapper
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            headers {
                frameOptions {
                    disable()
                }
            }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            csrf {
                disable()
            }
            httpBasic {
                disable()
            }
            anonymous {
                disable()
            }
            formLogin {
                disable()
            }
            logout {
                disable()
            }
            cors {
                CorsConfig()
            }

            authorizeHttpRequests {
                authorize("/admin/**", authenticated)
                authorize("/**", permitAll)
            }

            exceptionHandling {
                accessDeniedHandler = CustomAccessDeniedHandler(objectMapper)
                authenticationEntryPoint = CustomAuthenticationEntryPointHandler(objectMapper)
            }
        }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder()
    }
}
