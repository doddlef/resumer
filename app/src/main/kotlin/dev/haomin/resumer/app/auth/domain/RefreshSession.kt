package dev.haomin.resumer.app.auth.domain

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents a refresh session used for maintaining user authentication state and issuing new tokens.
 *
 * The refresh session contains various attributes for tracking its lifecycle, such as
 * creation and update timestamps, revocation status, and the secrets associated with it.
 * It also provides utility properties to determine if the session has been revoked
 * and compute the base timestamp for activity tracking.
 *
 * @property id The unique identifier of the refresh session.
 * @property accountId The unique identifier of the associated account.
 * @property secret The secret attached to this refresh session, used for authentication.
 * @property prevSecret The previous secret associated with the session, if applicable.
 * @property lastUsedAt The timestamp when the session was last used, or null if never used.
 * @property revokedAt The timestamp when the session was revoked, or null if it has not been revoked.
 * @property revokeReason The reason the session was revoked, if applicable.
 * @property createdAt The timestamp when the session was created.
 * @property updatedAt The timestamp when the session was last updated.
 */
data class RefreshSession(
    val id: UUID,
    val accountId: UUID,
    val secret: String,
    val prevSecret: String?,
    val lastUsedAt: OffsetDateTime?,
    val revokedAt: OffsetDateTime?,
    val revokeReason: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {

    /**
     * Indicates whether this session has been revoked.
     *
     * Returns `true` if the session's `revokedAt` timestamp is not null, signifying
     * that the session has been invalidated. Otherwise, returns `false`.
     */
    val isRevoked: Boolean
        get() = revokedAt != null

    /**
     * Computes the base timestamp used to determine session inactivity.
     *
     * This property returns the timestamp when the session was last used (`lastUsedAt`).
     * If the session has never been used (`lastUsedAt` is null), it falls back to the session's
     * creation timestamp (`createdAt`).
     *
     * Useful for tracking session idle duration or determining when to trigger expiration
     * or cleanup processes.
     */
    val idleBaseAt: OffsetDateTime
        get() = lastUsedAt ?: createdAt
}
