package dev.haomin.resumer.app.auth.principal

import dev.haomin.resumer.app.auth.domain.Account
import dev.haomin.resumer.app.auth.domain.AccountRole
import dev.haomin.resumer.app.auth.domain.AccountStatus
import java.util.UUID

/**
 * Represents an authenticated user principal derived from an `Account` entity.
 *
 * This class implements the `AuthPrincipal` interface, providing an abstraction
 * over the `Account` entity for authentication and authorization purposes. It
 * serves as the bridge between the application's domain objects and the security
 * framework by exposing properties required to identify and manage user access.
 *
 * @property account The `Account` instance encapsulated within this principal.
 *
 * The following properties and methods are sourced from the `Account` entity:
 * - `id` corresponds to the unique identifier of the associated account.
 * - `email` represents the email address of the account, utilized as the username.
 * - `status` reflects the current state of the account, such as active, archived, or locked.
 * - `role` determines the account's permissions within the system, such as user or admin.
 *
 * In addition, this class implements the following method:
 * - `getPassword`: Retrieves the hashed password of the account for authentication purposes.
 */
data class AccountPrincipal(
    val account: Account
): AuthPrincipal {
    override val id: UUID
        get() = account.id
    override val email: String
        get() = account.email
    override val status: AccountStatus
        get() = account.status
    override val role: AccountRole
        get() = account.role

    override fun getPassword(): String =
        account.password
}
