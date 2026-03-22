package dev.haomin.resumer.app.auth.service.impl

import dev.haomin.resumer.app.auth.principal.AuthPrincipal
import dev.haomin.resumer.app.auth.service.AuthService
import dev.haomin.resumer.app.auth.service.dto.EmailPwdLoginCmd
import dev.haomin.resumer.app.common.exception.AppException
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service

@Service
class AuthServiceImpl(
    private val authManager: AuthenticationManager,
): AuthService {
    companion object {
        private val logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)
    }

    override fun emailPasswordLogin(cmd: EmailPwdLoginCmd): AuthPrincipal {
        val (email, password) = cmd
        logger.debug("Login attempt for email: {}", email)

        val principal = UsernamePasswordAuthenticationToken(email, password)
            .let { authManager.authenticate(it) }
            .let {
                if (it.principal is AuthPrincipal)
                    it.principal as AuthPrincipal
                else {
                    val className = it.principal
                        ?.let { principal -> principal::class.simpleName }
                        ?: "unknown"
                    logger.error("Authentication principal is not of type AuthPrincipal, actual type: {}", className)
                    throw AppException()
                }
            }
        logger.debug("login successful for id: {}, email: {}", principal.id, principal.email)
        return principal
    }
}
