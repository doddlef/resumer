package dev.haomin.resumer.app.auth.filter

import dev.haomin.resumer.app.auth.AUTHORIZATION_HEADER
import dev.haomin.resumer.app.auth.BEAR_TOKEN_PREFIX
import dev.haomin.resumer.app.auth.service.TokenService
import jakarta.security.auth.message.AuthException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * Filter responsible for managing JWT-based authentication in the application.
 *
 * This filter is executed once per request and performs the following:
 * - Extracts the JWT token from the incoming HTTP request headers.
 * - Validates the token using the provided [TokenService].
 * - Creates an authenticated security context if the token is valid.
 * - Handles any authentication-related exceptions using the provided [HandlerExceptionResolver].
 *
 * @constructor Initializes the filter with the required dependencies.
 * @param tokenService The service used to validate and parse JWT tokens.
 * @param exceptionResolver The resolver used to handle exceptions during authentication.
 */
class JwtAuthFilter(
    private val tokenService: TokenService,
    private val exceptionResolver: HandlerExceptionResolver,
): OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain
    ) {
        val token = extractTokenFromRequest(request)

        try {
            if (!token.isNullOrBlank() && SecurityContextHolder.getContext().authentication == null) {
                val principal = tokenService.access(token)
                val authentication = UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.authorities
                )
                    .apply { this.details = WebAuthenticationDetailsSource().buildDetails(request) }
                SecurityContextHolder.createEmptyContext()
                    .apply { this.authentication = authentication }
                    .let { SecurityContextHolder.setContext(it) }
            }
        } catch (ex: AuthException) {
            SecurityContextHolder.clearContext()
            exceptionResolver.resolveException(request, response, null, ex)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromRequest(request: HttpServletRequest): String? =
        request.getHeader(AUTHORIZATION_HEADER)
            ?.trim()
            ?.takeIf { it.length > BEAR_TOKEN_PREFIX.length && it.startsWith(BEAR_TOKEN_PREFIX, ignoreCase = true) }
            ?.substring(BEAR_TOKEN_PREFIX.length)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}
