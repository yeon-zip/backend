package kr.ac.kumoh.polaris.bookvote.presentation.controller

import kr.ac.kumoh.polaris.auth.config.AppLoginAuthorizationRequestRepository
import kr.ac.kumoh.polaris.auth.handler.OAuth2AuthenticationFailureHandler
import kr.ac.kumoh.polaris.auth.handler.OAuth2AuthenticationSuccessHandler
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.auth.service.KakaoOidcUserService
import kr.ac.kumoh.polaris.auth.util.JwtTokenProvider
import kr.ac.kumoh.polaris.bookvote.service.BookVoteService
import kr.ac.kumoh.polaris.global.config.SecurityConfig
import kr.ac.kumoh.polaris.global.exception.GlobalExceptionHandler
import kr.ac.kumoh.polaris.bookvote.entity.BookVoteType
import kr.ac.kumoh.polaris.user.entity.UserRole
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(BookVoteController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class BookVoteControllerTest(
    @Autowired private val mockMvc: MockMvc
) {
    @MockitoBean
    private lateinit var bookVoteService: BookVoteService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var kakaoOidcUserService: KakaoOidcUserService

    @MockitoBean
    private lateinit var oauth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler

    @MockitoBean
    private lateinit var oauth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler

    @MockitoBean
    private lateinit var appLoginAuthorizationRequestRepository: AppLoginAuthorizationRequestRepository

    @MockitoBean
    private lateinit var clientRegistrationRepository: ClientRegistrationRepository

    @Test
    fun `unauthenticated user cannot vote book`() {
        mockMvc.perform(
            put("/api/v1/books/9791198363510/vote")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"voteType":"RECOMMEND"}
                """.trimIndent())
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
    }

    @Test
    fun `invalid voteType request returns bad request`() {
        mockMvc.perform(
            put("/api/v1/books/9791198363510/vote")
                .with(authentication(authenticatedUserAuthentication(7L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"voteType":"INVALID"}
                """.trimIndent())
        )
            .andExpect(status().isBadRequest)

        verifyNoInteractions(bookVoteService)
    }

    @Test
    fun `authenticated user can vote book with no content response`() {
        mockMvc.perform(
            put("/api/v1/books/9791198363510/vote")
                .with(authentication(authenticatedUserAuthentication(7L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"voteType":"RECOMMEND"}
                """.trimIndent())
        )
            .andExpect(status().isNoContent)

        verify(bookVoteService).voteBook(7L, "9791198363510", BookVoteType.RECOMMEND)
    }

    private fun authenticatedUserAuthentication(userId: Long): UsernamePasswordAuthenticationToken {
        val principal = AuthenticatedUser(id = userId, role = UserRole.USER)
        return UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_${UserRole.USER.name}"))
        )
    }
}
