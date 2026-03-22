package dev.haomin.resumer.app.auth

/**
 * The name of the HTTP cookie used to store the refresh token for user authentication.
 * This cookie is typically used in the context of session management to enable secure
 * and seamless token-based authentication workflows.
 */
const val REFRESH_TOKEN_COOKIE = "resumer_refresh"

/**
 * The name of the HTTP header used for carrying the authorization credentials.
 * Typically used in contexts where authentication information, such as a token,
 * needs to be included in the request headers for securing API endpoints.
 */
const val AUTHORIZATION_HEADER = "Authorization"

/**
 * The prefix used in the Authorization HTTP header to indicate that the included token
 * is a Bearer token. Bearer tokens are commonly used in token-based authentication
 * mechanisms to grant access to secured resources.
 */
const val BEAR_TOKEN_PREFIX = "Bearer "