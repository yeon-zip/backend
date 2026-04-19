package kr.ac.kumoh.polaris.auth.presentation.controller

import kr.ac.kumoh.polaris.auth.presentation.request.ExchangeCodeRequest
import kr.ac.kumoh.polaris.auth.presentation.response.AuthTokenResponse
import kr.ac.kumoh.polaris.auth.service.AppExchangeCodeService
import kr.ac.kumoh.polaris.auth.service.AuthTokenService
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.GlobalExceptionHandler
import kr.ac.kumoh.polaris.global.exception.ServiceException
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

class AuthControllerTest {
    private val authTokenService: AuthTokenService = mock()
    private val appExchangeCodeService: AppExchangeCodeService = mock()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(AuthController(authTokenService, appExchangeCodeService))
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    @Test
    fun `exchange endpoint returns auth tokens`() {
        whenever(appExchangeCodeService.exchange(eq(ExchangeCodeRequest("exchange-code", "mobile-app", verifier())))).thenReturn(
            AuthTokenResponse(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                expiresIn = 3600,
                userId = 1L
            )
        )

        mockMvc.perform(
            post("/api/v1/auth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "code" to "exchange-code",
                            "targetId" to "mobile-app",
                            "codeVerifier" to verifier()
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.userId").value(1L))
    }

    @Test
    fun `exchange endpoint surfaces problem detail errorCode on service failure`() {
        whenever(appExchangeCodeService.exchange(any())).thenThrow(ServiceException(ErrorCode.OIDC_PROOF_REQUIRED))

        mockMvc.perform(
            post("/api/v1/auth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "code" to "exchange-code",
                            "targetId" to "mobile-app"
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value(ErrorCode.OIDC_PROOF_REQUIRED.name))
    }

    private fun verifier(): String = "abcdefghijklmnopqrstuvwxyz0123456789-._~ABCDE"
}
