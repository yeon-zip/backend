package kr.ac.kumoh.polaris.auth.service

import kr.ac.kumoh.polaris.auth.entity.RefreshToken
import kr.ac.kumoh.polaris.auth.presentation.response.AuthTokenResponse
import kr.ac.kumoh.polaris.auth.repository.RefreshTokenRepository
import kr.ac.kumoh.polaris.auth.util.JwtTokenProvider
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthTokenService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository
) {
    fun issueTokens(user: User): AuthTokenResponse = issueTokensWithRefreshToken(user).response

    fun issueTokensWithRefreshToken(user: User): IssuedTokens {
        val accessToken = jwtTokenProvider.generateAccessToken(user)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user)
        val expiresAt = jwtTokenProvider.getExpiresAt(jwtTokenProvider.parseRefreshToken(refreshToken))

        val persistedRefreshToken = refreshTokenRepository.save(
            RefreshToken(
                token = refreshToken,
                expiresAt = expiresAt,
                user = user
            )
        )

        return IssuedTokens(
            response = AuthTokenResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = newAccessTokenDurationSeconds(accessToken),
                userId = user.id ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)
            ),
            refreshTokenId = persistedRefreshToken.id
        )
    }

    fun refresh(authorizationHeader: String): AuthTokenResponse {
        val token = JwtTokenProvider.resolveBearerToken(authorizationHeader)
            ?: throw ServiceException(ErrorCode.INVALID_TOKEN, "refresh token이 필요합니다.")

        val parsedRefreshToken = jwtTokenProvider.parseRefreshToken(token)
        val persistedRefreshToken = refreshTokenRepository.findByToken(token)
            ?: throw ServiceException(ErrorCode.REFRESH_TOKEN_NOT_FOUND, "저장된 refresh token이 없습니다.")

        if (persistedRefreshToken.isExpired()) {
            refreshTokenRepository.delete(persistedRefreshToken)
            throw ServiceException(ErrorCode.REFRESH_TOKEN_EXPIRED, "만료된 refresh token입니다.")
        }

        val userId = jwtTokenProvider.getUserId(parsedRefreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { ServiceException(ErrorCode.USER_NOT_FOUND) }

        val newAccessToken = jwtTokenProvider.generateAccessToken(user)
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(user)
        val expiresAt = jwtTokenProvider.getExpiresAt(jwtTokenProvider.parseRefreshToken(newRefreshToken))

        persistedRefreshToken.rotate(newRefreshToken, expiresAt)

        return AuthTokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = newAccessTokenDurationSeconds(newAccessToken),
            userId = userId
        )
    }

    fun logout(
        userId: Long,
        refreshTokenHeader: String?
    ) {
        val refreshToken = JwtTokenProvider.resolveBearerToken(refreshTokenHeader)

        if (refreshToken == null) {
            refreshTokenRepository.deleteAllByUserId(userId)
            return
        }

        val persistedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
            ?: return

        if (persistedRefreshToken.user.id != userId) {
            throw ServiceException(ErrorCode.INVALID_TOKEN, "다른 사용자의 refresh token입니다.")
        }

        refreshTokenRepository.delete(persistedRefreshToken)
    }

    data class IssuedTokens(
        val response: AuthTokenResponse,
        val refreshTokenId: Long?
    )

    private fun newAccessTokenDurationSeconds(accessToken: String): Long {
        val jwt = jwtTokenProvider.parseAccessToken(accessToken)
        return (jwt.expiresAt?.epochSecond ?: 0) - (jwt.issuedAt?.epochSecond ?: 0)
    }
}
