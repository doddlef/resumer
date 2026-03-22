package dev.haomin.resumer.app.auth.service.dto

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents the result of a session refresh operation.
 *
 * @property accountId The unique identifier of the account associated with the session.
 * @property refreshToken The new refresh token generated for the session.
 * @property expiry The timestamp indicating when the new refresh token will expire.
 */
data class RefreshSessionResult(
    val accountId: UUID,
    val refreshToken: String,
    val expiry: OffsetDateTime,
)
