package dev.haomin.resumer.app.auth.principal

import dev.haomin.resumer.app.auth.exception.AuthRequiredException
import java.util.UUID

/**
 * Defines a provider for accessing and managing the currently authenticated principal.
 *
 * This interface offers methods to retrieve the current authenticated principal (`AuthPrincipal`),
 * enforce validation of its presence, and access its unique identifier.
 */
interface PrincipalProvider {

    /**
     * Retrieves the currently authenticated principal.
     *
     * This method provides access to the `AuthPrincipal` representing
     * the authenticated user, if one is available in the current
     * security context.
     *
     * @return The currently authenticated principal as an `AuthPrincipal`, or `null` if no principal is authenticated.
     */
    fun current(): AuthPrincipal?

    /**
     * Validates and retrieves the currently authenticated principal.
     *
     * This method enforces the presence of an authenticated principal in the security context.
     * If no principal is authenticated, an exception is thrown.
     *
     * @return The currently authenticated principal as an `AuthPrincipal`.
     * @throws [AuthRequiredException] if no principal is authenticated.
     */
    fun requireCurrent(): AuthPrincipal =
        current() ?: throw AuthRequiredException()

    /**
     * Enforces validation of the presence of an authenticated principal and retrieves its unique identifier.
     *
     * This method ensures that a principal is authenticated in the current security context. If no
     * principal is authenticated, an exception is thrown. Once the validation succeeds, the unique
     * identifier (UUID) of the authenticated principal is returned.
     *
     * @return The unique identifier (UUID) of the currently authenticated principal.
     * @throws [AuthRequiredException] if no principal is authenticated.
     */
    fun requireCurrentId(): UUID =
        requireCurrent().id
}