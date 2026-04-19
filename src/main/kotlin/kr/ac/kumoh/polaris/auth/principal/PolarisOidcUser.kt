package kr.ac.kumoh.polaris.auth.principal

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import kr.ac.kumoh.polaris.user.entity.User

class PolarisOidcUser(
    val user: User,
    authorities: Collection<GrantedAuthority>,
    idToken: OidcIdToken,
    userInfo: OidcUserInfo?
) : DefaultOidcUser(authorities, idToken, userInfo)
