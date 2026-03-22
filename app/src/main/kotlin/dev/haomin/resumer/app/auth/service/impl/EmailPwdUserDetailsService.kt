package dev.haomin.resumer.app.auth.service.impl

import dev.haomin.resumer.app.auth.principal.AccountPrincipal
import dev.haomin.resumer.app.auth.repo.AccountRepo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.*
import org.springframework.stereotype.Service

/**
 * UserDetailsService implementation for email and password authentication.
 */
@Service
class EmailPwdUserDetailsService(
    private val repo: AccountRepo,
): UserDetailsService {

    /**
     * Loads the user details by username (email in this case).
     *
     * @param username the email of the user
     * @return UserDetails object containing user information
     */
    override fun loadUserByUsername(username: String): UserDetails =
        repo.selectByEmail(username)
            ?.let {
                logger.debug("UserDetails loaded for email={}", username)
                AccountPrincipal(it)
            }
            ?: run {
                logger.warn("UserDetails lookup failed for email={}", username)
                throw UsernameNotFoundException("User with email $username not found")
            }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(EmailPwdUserDetailsService::class.java)
    }
}
