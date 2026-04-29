package kr.ac.kumoh.polaris.auth.service

import kr.ac.kumoh.polaris.auth.config.AppLoginAuthorizationRequestRepository
import kr.ac.kumoh.polaris.auth.config.AppLoginChannel
import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import java.time.Duration

class AppLoginResolverTest {
    @Test
    fun `web login defaults target to web and binds PKCE proof`() {
        val repository = AppLoginAuthorizationRequestRepository()
        val resolver = AppLoginResolver(authAppLoginProperties(), repository)
        val request = MockHttpServletRequest()

        val redirectPath = resolver.resolveLoginRedirect(
            request = request,
            channelValue = null,
            targetIdValue = null,
            codeChallengeValue = VALID_CODE_CHALLENGE,
            codeChallengeMethodValue = "S256"
        )

        assertEquals("/oauth2/authorization/kakao", redirectPath)

        val savedContext = repository.loadAppLoginContext(request)
        assertEquals(AppLoginChannel.WEB, savedContext?.channel)
        assertEquals("web", savedContext?.targetId)
        assertEquals(VALID_CODE_CHALLENGE, savedContext?.codeChallenge)
        assertEquals("S256", savedContext?.codeChallengeMethod)
    }

    @Test
    fun `web login requires PKCE proof for exchange flow`() {
        val repository = AppLoginAuthorizationRequestRepository()
        val resolver = AppLoginResolver(authAppLoginProperties(), repository)
        val request = MockHttpServletRequest()

        val exception = assertThrows(ServiceException::class.java) {
            resolver.resolveLoginRedirect(
                request = request,
                channelValue = "web",
                targetIdValue = "web",
                codeChallengeValue = null,
                codeChallengeMethodValue = null
            )
        }

        assertEquals(ErrorCode.OIDC_PROOF_REQUIRED, exception.errorCode)
    }

    @Test
    fun `web login rejects unknown target`() {
        val repository = AppLoginAuthorizationRequestRepository()
        val resolver = AppLoginResolver(authAppLoginProperties(), repository)
        val request = MockHttpServletRequest()

        val exception = assertThrows(ServiceException::class.java) {
            resolver.resolveLoginRedirect(
                request = request,
                channelValue = "web",
                targetIdValue = "desktop",
                codeChallengeValue = VALID_CODE_CHALLENGE,
                codeChallengeMethodValue = "S256"
            )
        }

        assertEquals(ErrorCode.OIDC_TARGET_NOT_ALLOWED, exception.errorCode)
    }

    private fun authAppLoginProperties(): AuthAppLoginProperties = AuthAppLoginProperties(
        app = AuthAppLoginProperties.App(
            targets = mapOf(
                "web" to "http://localhost:3000/login",
                "mobile" to "polaris://auth/callback"
            ),
            exchange = AuthAppLoginProperties.Exchange(
                ttl = Duration.ofMinutes(3),
                allowWithoutProof = false
            )
        )
    )

    companion object {
        private const val VALID_CODE_CHALLENGE = "abcdefghijklmnopqrstuvwxyzABCDE1234567890_-"
    }
}
