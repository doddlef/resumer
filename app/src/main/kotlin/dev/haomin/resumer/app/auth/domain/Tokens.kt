package dev.haomin.resumer.app.auth.domain

import java.time.OffsetDateTime

/**
 * Represents an authentication token with its value and expiration time.
 *
 * This data class is typically used to encapsulate information about an
 * authentication token, including the token string itself and the
 * time at which it expires.
 *
 * @property token The token as a string value.
 * @property expiry The expiration time of the token as an OffsetDateTime.
 */
data class Token(
    val token: String,
    val expiry: OffsetDateTime,
)

/**
 * Represents a pair of authentication tokens: one for access and one for refresh.
 *
 * This data class encapsulates both the access token, used to authenticate
 * and authorize requests to a service, and the refresh token, used to obtain
 * new access tokens when they expire.
 *
 * @property access The access token used for authentication purposes.
 * @property refresh The refresh token used to obtain a new access token.
 */
data class TokenPair(
    val access: Token,
    val refresh: Token,
)