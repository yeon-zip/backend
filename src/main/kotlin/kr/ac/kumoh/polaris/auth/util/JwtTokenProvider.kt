package kr.ac.kumoh.polaris.auth.util

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import kr.ac.kumoh.polaris.auth.config.JwtProperties
import kr.ac.kumoh.polaris.global.exception.ErrorCode
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.user.entity.User
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) {
    private val secretKey: SecretKey = SecretKeySpec(
        jwtProperties.secret.toByteArray(StandardCharsets.UTF_8),
        "HmacSHA256"
    )
    private val encoder: JwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey.encoded))
    private val decoder: JwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build()

    init {
        require(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8).size >= 32) {
            "JWT secret must be at least 32 bytes."
        }
    }

    fun generateAccessToken(user: User): String = generateToken(
        userId = user.id ?: throw ServiceException(ErrorCode.USER_NOT_FOUND),
        role = user.role.name,
        type = TokenType.ACCESS,
        expirationSeconds = jwtProperties.accessTokenExpirationSeconds
    )

    fun generateRefreshToken(user: User): String = generateToken(
        userId = user.id ?: throw ServiceException(ErrorCode.USER_NOT_FOUND),
        role = user.role.name,
        type = TokenType.REFRESH,
        expirationSeconds = jwtProperties.refreshTokenExpirationSeconds,
        jwtId = UUID.randomUUID().toString()
    )

    fun parseAccessToken(token: String): Jwt = parse(token, TokenType.ACCESS)

    fun parseRefreshToken(token: String): Jwt = parse(token, TokenType.REFRESH)

    fun getUserId(jwt: Jwt): Long = jwt.subject.toLong()

    fun getExpiresAt(jwt: Jwt): LocalDateTime = LocalDateTime.ofInstant(
        jwt.expiresAt ?: throw ServiceException(ErrorCode.INVALID_TOKEN),
        ZoneId.systemDefault()
    )

    private fun parse(token: String, expectedType: TokenType): Jwt {
        val jwt = try {
            decoder.decode(token)
        } catch (exception: Exception) {
            throw ServiceException(ErrorCode.INVALID_TOKEN, "유효하지 않은 토큰입니다.")
        }

        if (jwt.issuer?.toString() != jwtProperties.issuer) {
            throw ServiceException(ErrorCode.INVALID_TOKEN, "토큰 발급자가 올바르지 않습니다.")
        }

        if (jwt.getClaimAsString(CLAIM_TOKEN_TYPE) != expectedType.value) {
            throw ServiceException(ErrorCode.INVALID_TOKEN, "토큰 유형이 올바르지 않습니다.")
        }

        return jwt
    }

    private fun generateToken(
        userId: Long,
        role: String,
        type: TokenType,
        expirationSeconds: Long,
        jwtId: String? = null
    ): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expirationSeconds))
            .subject(userId.toString())
            .claim(CLAIM_ROLE, role)
            .claim(CLAIM_TOKEN_TYPE, type.value)
            .apply {
                jwtId?.let(::id)
            }
            .build()

        val headers = JwsHeader.with(MacAlgorithm.HS256).build()
        return encoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
    }

    companion object {
        private const val CLAIM_ROLE = "role"
        private const val CLAIM_TOKEN_TYPE = "token_type"

        fun resolveBearerToken(authorizationHeader: String?): String? {
            if (authorizationHeader.isNullOrBlank()) {
                return null
            }

            return if (authorizationHeader.startsWith("Bearer ", ignoreCase = true)) {
                authorizationHeader.substring(7)
            } else {
                null
            }
        }
    }
}

enum class TokenType(val value: String) {
    ACCESS("access"),
    REFRESH("refresh")
}
