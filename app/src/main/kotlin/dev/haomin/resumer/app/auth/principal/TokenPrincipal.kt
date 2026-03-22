package dev.haomin.resumer.app.auth.principal

import dev.haomin.resumer.app.auth.domain.AccountRole
import dev.haomin.resumer.app.auth.domain.AccountStatus
import java.util.UUID

/**
 * Represents an authenticated principal derived from a token-based authentication mechanism.
 *
 * This class implements the `AuthPrincipal` interface, providing a lightweight implementation
 * for scenarios where user authentication is handled via tokens. Unlike other implementations,
 * the `TokenPrincipal` does not store or manage password information.
 *
 * The following properties are defined as part of the `AuthPrincipal` contract:
 * - `id`: A unique identifier (UUID) representing the principal.
 * - `email`: The email address associated with the principal, used as the username.
 * - `status`: The current account status, indicating the principal's access state (e.g., active, locked).
 * - `role`: The role assigned to the principal, determining their access permissions (e.g., user, admin).
 *
 * Additionally, this implementation:
 * - Overrides the `getPassword` method to return `null`, as password information
 *   is not applicable or stored for token-based authentication.
 */
data class TokenPrincipal(
    override val id: UUID,
    override val email: String,
    override val status: AccountStatus,
    override val role: AccountRole,
): AuthPrincipal {
    override fun getPassword(): String? = null
}
