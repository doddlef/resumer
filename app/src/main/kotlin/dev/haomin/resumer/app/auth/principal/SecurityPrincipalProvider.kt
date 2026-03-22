package dev.haomin.resumer.app.auth.principal

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

/**
 * Implementation of the `PrincipalProvider` that retrieves the currently authenticated principal
 * from the Spring Security context.
 *
 * This class integrates with Spring Security's `SecurityContextHolder` to fetch the
 * current authentication details and extract the `AuthPrincipal`. It serves as a bridge
 * for accessing application-specific principal information within the context of a
 * secured request.
 *
 * Key Responsibilities:
 * - Accessing the `SecurityContextHolder` to retrieve the current security context.
 * - Extracting the authenticated principal (`AuthPrincipal`) from the security context.
 * - Converting the principal to the `AuthPrincipal` implementation or returning `null`
 *   if no authenticated principal exists.
 */
@Service
class SecurityPrincipalProvider: PrincipalProvider {
    override fun current(): AuthPrincipal? =
        SecurityContextHolder.getContext()
            .authentication
            ?.principal
            ?.let { it as AuthPrincipal }
}
