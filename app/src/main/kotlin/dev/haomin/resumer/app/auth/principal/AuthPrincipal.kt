package dev.haomin.resumer.app.auth.principal

import dev.haomin.resumer.app.auth.domain.AccountRole
import dev.haomin.resumer.app.auth.domain.AccountStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

/**
 * Represents an authenticated principal in the system.
 *
 * This interface extends the Spring Security `UserDetails` interface
 * and adds additional properties specific to the application's authentication needs.
 *
 * @property id The unique identifier (UUID) of the principal.
 * @property email The email address of the principal, serving as their username.
 * @property status The current account status of the principal, determining their access level.
 * @property role The role of the principal, determining their permissions.
 *
 * In addition to the properties, this interface provides custom implementations of the
 * `UserDetails` methods to align with the authentication and authorization requirements.
 * - `getUsername`: Returns the email address as the username.
 * - `getAuthorities`: Provides authorities based on the principal's status and role,
 *   formatted as `STATUS_{STATUS_NAME}` and `ROLE_{ROLE_NAME}` respectively.
 * - `isAccountNonLocked`: Determines if the account is non-locked,
 *   where accounts with a `LOCKED` status are considered locked.
 * - `isAccountNonExpired`: Determines if the account is non-expired,
 *   where accounts with an `ARCHIVED` status are considered expired.
 */
interface AuthPrincipal: UserDetails {
    val id: UUID
    val email: String
    val status: AccountStatus
    val role: AccountRole

    override fun getUsername(): String =
        this.email

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf("STATUS_${status.name}", "ROLE_${role.name}").map { SimpleGrantedAuthority(it) }

    override fun isAccountNonLocked(): Boolean =
        status != AccountStatus.LOCKED

    override fun isAccountNonExpired(): Boolean =
        status != AccountStatus.ARCHIVED
}