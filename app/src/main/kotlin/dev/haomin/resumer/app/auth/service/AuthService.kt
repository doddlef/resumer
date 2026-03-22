package dev.haomin.resumer.app.auth.service

import dev.haomin.resumer.app.auth.principal.AuthPrincipal
import dev.haomin.resumer.app.auth.service.dto.EmailPwdLoginCmd

/**
 * Service interface for handling authentication workflows.
 *
 * Provides methods for authenticating users via email and password.
 */
interface AuthService {
    /**
     * Authenticate a user using email and password.
     *
     * @param cmd Command object containing email and password.
     * @return AuthPrincipal representing the authenticated user.
     */
    fun emailPasswordLogin(cmd: EmailPwdLoginCmd): AuthPrincipal
}