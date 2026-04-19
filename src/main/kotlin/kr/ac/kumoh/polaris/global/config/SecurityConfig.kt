package kr.ac.kumoh.polaris.global.config

import kr.ac.kumoh.polaris.auth.filter.JwtAuthenticationFilter
import kr.ac.kumoh.polaris.auth.handler.OAuth2AuthenticationFailureHandler
import kr.ac.kumoh.polaris.auth.handler.OAuth2AuthenticationSuccessHandler
import kr.ac.kumoh.polaris.auth.service.KakaoOidcUserService
import kr.ac.kumoh.polaris.auth.util.JwtTokenProvider
import kr.ac.kumoh.polaris.global.handler.CustomAccessDeniedHandler
import kr.ac.kumoh.polaris.global.handler.CustomAuthenticationEntryPointHandler
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val objectMapper: ObjectMapper,
    private val jwtTokenProvider: JwtTokenProvider,
    private val kakaoOidcUserService: KakaoOidcUserService,
    private val oauth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val oauth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler
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
                sessionCreationPolicy = SessionCreationPolicy.IF_REQUIRED
            }
            csrf {
                disable()
            }
            httpBasic {
                disable()
            }
            formLogin {
                disable()
            }
            cors {
                CorsConfig()
            }
            authorizeHttpRequests {
                authorize("/api/v1/auth/kakao/login", permitAll)
                authorize("/api/v1/auth/exchange", permitAll)
                authorize("/api/v1/auth/refresh", permitAll)
                authorize("/oauth2/**", permitAll)
                authorize("/login/oauth2/**", permitAll)
                authorize("/api/v1/auth/logout", authenticated)
                authorize("/api/v1/users/me", authenticated)
                authorize("/admin/**", authenticated)
                authorize("/**", permitAll)
            }
            exceptionHandling {
                accessDeniedHandler = CustomAccessDeniedHandler(objectMapper)
                authenticationEntryPoint = CustomAuthenticationEntryPointHandler(objectMapper)
            }
        }


        http.oauth2Login { oauth2 ->
            oauth2.userInfoEndpoint { userInfo ->
                userInfo.oidcUserService(kakaoOidcUserService)
            }
            oauth2.successHandler(oauth2AuthenticationSuccessHandler)
            oauth2.failureHandler(oauth2AuthenticationFailureHandler)
        }

        http.addFilterBefore(
            JwtAuthenticationFilter(jwtTokenProvider, objectMapper),
            UsernamePasswordAuthenticationFilter::class.java
        )

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
}
