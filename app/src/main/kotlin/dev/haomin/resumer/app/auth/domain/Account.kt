package dev.haomin.resumer.app.auth.domain

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents the status of an account in the application.
 *
 * The `AccountStatus` enumeration defines the various states
 * an account can be in. These states indicate whether the account
 * is currently active, archived, or locked.
 *
 * Enum Constants:
 * - ACTIVE: Indicates the account is active and accessible.
 * - ARCHIVED: Indicates the account is archived and not accessible for regular operations.
 * - LOCKED: Indicates the account is locked, potentially due to security or policy reasons.
 *
 * Companion Object Functions:
 * - fromString(value: String): Parses a string to match an `AccountStatus` value,
 *   ignoring case differences. If the string does not match any enum constant,
 *   an `IllegalArgumentException` is thrown.
 */
enum class AccountStatus {
    ACTIVE,
    ARCHIVED,
    LOCKED;

    companion object {
        fun fromString(value: String): AccountStatus =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid AccountStatus: $value")
    }
}

/**
 * Represents the role assigned to an account in the application.
 *
 * The `AccountRole` enumeration defines the access level of an account,
 * determining the permissions and operations that can be performed.
 *
 * Enum Constants:
 * - USER: Represents a standard user role with limited permissions.
 * - ADMIN: Represents an administrative role with elevated permissions.
 *
 * Companion Object Functions:
 * - fromString(value: String): Parses a string to match an `AccountRole` value,
 *   ignoring case differences. If the string does not match any enum constant,
 *   an `IllegalArgumentException` is thrown.
 */
enum class AccountRole {
    USER,
    ADMIN;

    companion object {
        fun fromString(value: String): AccountRole =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid AccountRole: $value")
    }
}

/**
 * Represents an account in the application.
 *
 * The `Account` class encapsulates information about a user account, including
 * identification, authentication, role, status, and timestamps for creation
 * and updates. It also provides a method to transform the account instance
 * into a profile representation.
 *
 * @property id The unique identifier of the account.
 * @property email The email address associated with the account.
 * @property password The hashed password of the account (secured).
 * @property name The display name of the account holder.
 * @property status The current status of the account (e.g., active, archived, or locked).
 * @property role The role assigned to the account (e.g., user or admin).
 * @property createdAt The timestamp when the account was created.
 * @property updatedAt The timestamp when the account was last updated.
 */
data class Account(
    val id: UUID,
    val email: String,
    val password: String,
    val name: String,
    val status: AccountStatus,
    val role: AccountRole,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    fun asProfile(): AccountProfile =
        AccountProfile(
            id = id,
            email = email,
            name = name,
            status = status,
            role = role,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}

/**
 * Represents a read-only profile view of an account in the application.
 *
 * The `AccountProfile` class provides a structured representation of
 * essential account details without including sensitive information
 * such as the account's password. This class is commonly used
 * for displaying account information in contexts where security
 * and privacy are critical.
 *
 * @property id The unique identifier of the account.
 * @property email The email address associated with the account.
 * @property name The display name of the account holder.
 * @property status The current status of the account, indicating whether
 *                  it is active, archived, or locked.
 * @property role The role assigned to the account, determining
 *                the permissions available to the account holder.
 * @property createdAt The timestamp of when the account was created.
 * @property updatedAt The timestamp of the most recent update to the account.
 */
data class AccountProfile(
    val id: UUID,
    val email: String,
    val name: String,
    val status: AccountStatus,
    val role: AccountRole,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)