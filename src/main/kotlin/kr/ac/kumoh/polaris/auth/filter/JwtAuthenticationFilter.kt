package kr.ac.kumoh.polaris.auth.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.ac.kumoh.polaris.auth.principal.AuthenticatedUser
import kr.ac.kumoh.polaris.auth.util.JwtTokenProvider
import kr.ac.kumoh.polaris.global.exception.ServiceException
import kr.ac.kumoh.polaris.user.entity.UserRole
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path == "/api/v1/auth/refresh" ||
            path == "/api/v1/auth/exchange" ||
            path.startsWith("/oauth2/") ||
            path.startsWith("/login/oauth2/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val bearerToken = JwtTokenProvider.resolveBearerToken(request.getHeader(AUTHORIZATION_HEADER))

        if (bearerToken == null) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val jwt = jwtTokenProvider.parseAccessToken(bearerToken)
            val authenticatedUser = AuthenticatedUser(
                id = jwtTokenProvider.getUserId(jwt),
                role = UserRole.valueOf(jwt.getClaimAsString("role"))
            )
            val authentication = UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                listOf(SimpleGrantedAuthority("ROLE_${authenticatedUser.role.name}"))
            )
            SecurityContextHolder.getContext().authentication = authentication
            filterChain.doFilter(request, response)
        } catch (exception: ServiceException) {
            SecurityContextHolder.clearContext()
            val problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED).apply {
                title = HttpStatus.UNAUTHORIZED.reasonPhrase
                detail = exception.message
                setProperty("errorCode", exception.errorCode.name)
            }
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
            response.characterEncoding = Charsets.UTF_8.name()
            objectMapper.writeValue(response.writer, problemDetail)
        }
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
    }
}
