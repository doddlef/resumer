package dev.haomin.resumer.app.auth.service

import dev.haomin.resumer.app.auth.service.dto.RefreshSessionResult
import java.util.UUID

/**
 * Service interface responsible for managing refresh sessions,
 * such as creating, rotating, and revoking them.
 */
interface RefreshService {

    /**
     * Creates a new refresh session for the provided account identifier.
     *
     * @param accountId The unique identifier of the account for which the refresh session is to be created.
     * @return A result containing the details of the created refresh session, including the account ID,
     *         refresh token, and session expiry timestamp.
     */
    fun createSession(accountId: UUID): RefreshSessionResult

    /**
     * Rotates an existing refresh session by invalidating the current refresh token
     * and generating a new one. The operation returns details about the updated session,
     * including the associated account ID, new refresh token, and its expiry timestamp.
     *
     * @param refreshToken The current refresh token that needs to be rotated.
     * @return A result containing the details of the updated refresh session.
     */
    fun rotateSession(refreshToken: String): RefreshSessionResult

    /**
     * Revokes an active refresh session associated with the provided refresh token.
     * This operation invalidates the refresh token, preventing further use.
     *
     * @param refreshToken The refresh token that identifies the session to be revoked.
     * @param reason The reason for revoking the session, typically for auditing or accountability purposes.
     * @return A boolean indicating whether the session was successfully revoked. Returns true if the token was revoked, false otherwise.
     */
    fun revokeSession(refreshToken: String, reason: String): Boolean
}
