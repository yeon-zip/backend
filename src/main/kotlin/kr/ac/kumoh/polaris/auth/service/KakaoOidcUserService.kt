package kr.ac.kumoh.polaris.auth.service

import kr.ac.kumoh.polaris.auth.principal.PolarisOidcUser
import kr.ac.kumoh.polaris.user.entity.User
import kr.ac.kumoh.polaris.user.entity.UserAuthProvider
import kr.ac.kumoh.polaris.user.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KakaoOidcUserService(
    private val userRepository: UserRepository
) : OAuth2UserService<OidcUserRequest, OidcUser> {
    private val delegate = OidcUserService()

    @Transactional
    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = delegate.loadUser(userRequest)
        val issuer = userRequest.idToken.issuer.toString()
        val subject = userRequest.idToken.subject

        if (subject.isBlank()) {
            throw OAuth2AuthenticationException(OAuth2Error("invalid_token"), "OIDC sub claim이 없습니다.")
        }

        val user = userRepository.findByOidcIssuerAndOidcSubject(issuer, subject)
            ?.apply {
                updateProfile(
                    nickname = extractNickname(oidcUser),
                    email = oidcUser.email,
                    profileImageUrl = extractProfileImageUrl(oidcUser)
                )
            }
            ?: userRepository.save(
                User(
                    provider = UserAuthProvider.KAKAO,
                    oidcIssuer = issuer,
                    oidcSubject = subject,
                    nickname = extractNickname(oidcUser),
                    email = oidcUser.email,
                    profileImageUrl = extractProfileImageUrl(oidcUser)
                )
            )

        val authorities = oidcUser.authorities + SimpleGrantedAuthority("ROLE_${user.role.name}")
        return PolarisOidcUser(
            user = user,
            authorities = authorities,
            idToken = oidcUser.idToken,
            userInfo = oidcUser.userInfo
        )
    }

    private fun extractNickname(oidcUser: OidcUser): String? =
        oidcUser.getClaimAsString("nickname")
            ?: (oidcUser.getClaimAsMap("properties")?.get("nickname") as? String)
            ?: oidcUser.fullName

    private fun extractProfileImageUrl(oidcUser: OidcUser): String? =
        oidcUser.getClaimAsString("picture")
            ?: (oidcUser.getClaimAsMap("properties")?.get("profile_image") as? String)
}
