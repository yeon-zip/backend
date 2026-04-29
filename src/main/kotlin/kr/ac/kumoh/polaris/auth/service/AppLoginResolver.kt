package kr.ac.kumoh.polaris.auth.service

import jakarta.servlet.http.HttpServletRequest
import kr.ac.kumoh.polaris.auth.config.AppLoginAuthorizationRequestRepository
import kr.ac.kumoh.polaris.auth.config.AppLoginChannel
import kr.ac.kumoh.polaris.auth.config.AppLoginContext
import kr.ac.kumoh.polaris.auth.config.AuthAppLoginProperties
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
/**
 * 웹/앱 공통 OIDC 로그인 시작 파라미터를 검증하고,
 * OAuth 성공 후 exchange code 발급에 필요한 컨텍스트를 세션에 저장한다.
 */
class AppLoginResolver(
    private val authAppLoginProperties: AuthAppLoginProperties,
    private val appLoginAuthorizationRequestRepository: AppLoginAuthorizationRequestRepository
) {
    fun resolveLoginRedirect(
        request: HttpServletRequest,
        channelValue: String?,
        targetIdValue: String?,
        codeChallengeValue: String?,
        codeChallengeMethodValue: String?
    ): String {
        val channel = AppLoginChannel.from(channelValue)
        val codeChallenge = codeChallengeValue?.trim()?.takeIf { it.isNotEmpty() }
        val codeChallengeMethod = codeChallengeMethodValue?.trim()?.takeIf { it.isNotEmpty() }

        val appLoginContext = when (channel) {
            AppLoginChannel.APP -> buildAppContext(targetIdValue, codeChallenge, codeChallengeMethod)
            AppLoginChannel.WEB -> buildWebContext(targetIdValue, codeChallenge, codeChallengeMethod)
        }

        appLoginAuthorizationRequestRepository.saveAppLoginContext(request, appLoginContext)
        return KAKAO_AUTHORIZATION_ENDPOINT
    }

    fun resolveTargetUri(targetId: String): String = authAppLoginProperties.resolveTarget(targetId)
        ?: throw ServiceException(
            ErrorCode.OIDC_TARGET_NOT_ALLOWED,
            "허용되지 않은 로그인 대상입니다: $targetId"
        )

    private fun buildAppContext(
        targetIdValue: String?,
        codeChallenge: String?,
        codeChallengeMethod: String?
    ): AppLoginContext = buildLoginContext(
        channel = AppLoginChannel.APP,
        targetId = targetIdValue?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw ServiceException(ErrorCode.OIDC_TARGET_NOT_ALLOWED, "앱 로그인 대상 target이 필요합니다."),
        codeChallenge = codeChallenge,
        codeChallengeMethod = codeChallengeMethod
    )

    private fun buildWebContext(
        targetIdValue: String?,
        codeChallenge: String?,
        codeChallengeMethod: String?
    ): AppLoginContext = buildLoginContext(
        channel = AppLoginChannel.WEB,
        targetId = targetIdValue?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_WEB_TARGET_ID,
        codeChallenge = codeChallenge,
        codeChallengeMethod = codeChallengeMethod
    )

    private fun buildLoginContext(
        channel: AppLoginChannel,
        targetId: String,
        codeChallenge: String?,
        codeChallengeMethod: String?
    ): AppLoginContext {
        resolveTargetUri(targetId)
        validateProofRequirements(codeChallenge, codeChallengeMethod)

        return AppLoginContext(
            channel = channel,
            targetId = targetId,
            codeChallenge = codeChallenge,
            codeChallengeMethod = codeChallengeMethod,
            correlationId = UUID.randomUUID().toString()
        )
    }

    private fun validateProofRequirements(
        codeChallenge: String?,
        codeChallengeMethod: String?
    ) {
        if (codeChallenge == null && !authAppLoginProperties.app.exchange.allowWithoutProof) {
            throw ServiceException(ErrorCode.OIDC_PROOF_REQUIRED, "로그인에는 PKCE S256 codeChallenge가 필요합니다.")
        }

        if (codeChallenge != null || codeChallengeMethod != null) {
            validateChallenge(codeChallenge, codeChallengeMethod)
        }
    }

    private fun validateChallenge(
        codeChallenge: String?,
        codeChallengeMethod: String?
    ) {
        if (codeChallenge.isNullOrBlank()) {
            throw ServiceException(ErrorCode.OIDC_PROOF_REQUIRED, "codeChallenge가 필요합니다.")
        }
        if (codeChallengeMethod != REQUIRED_CODE_CHALLENGE_METHOD) {
            throw ServiceException(
                ErrorCode.OIDC_UNSUPPORTED_CODE_CHALLENGE_METHOD,
                "지원하지 않는 codeChallengeMethod입니다: ${codeChallengeMethod ?: "null"}"
            )
        }
        if (!CODE_CHALLENGE_PATTERN.matches(codeChallenge)) {
            throw ServiceException(ErrorCode.OIDC_INVALID_CODE_CHALLENGE, "codeChallenge는 base64url 형식이어야 합니다.")
        }
    }

    companion object {
        private const val KAKAO_AUTHORIZATION_ENDPOINT = "/oauth2/authorization/kakao"
        private const val DEFAULT_WEB_TARGET_ID = "web"
        private const val REQUIRED_CODE_CHALLENGE_METHOD = "S256"
        private val CODE_CHALLENGE_PATTERN = Regex("^[A-Za-z0-9_-]{43,128}$")
    }
}
