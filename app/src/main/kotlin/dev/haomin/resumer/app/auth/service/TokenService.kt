package dev.haomin.resumer.app.auth.service

import dev.haomin.resumer.app.auth.principal.TokenPrincipal
import dev.haomin.resumer.app.auth.service.dto.TokenIssueResult
import java.util.UUID

/**
 * Service interface for managing token-based authentication workflows.
 *
 * Provides methods for generating, refreshing, validating, and revoking tokens
 * to ensure secure access to the system. Each method is responsible for a specific
 * stage of the token lifecycle.
 */
interface TokenService {

    /**
     * Authenticates an account using its unique identifier and issues a new pair of tokens
     * for managing session authentication. This method initiates the login process
     * and generates a token pair comprising an access token and a refresh token.
     *
     * @param accountId The unique identifier of the account that needs to authenticate.
     * @return A `TokenIssueResult` containing the issued token pair and the account profile.
     */
    fun login(accountId: UUID): TokenIssueResult

    /**
     * Refreshes an authentication session by rotating the provided refresh token
     * and issuing new access and refresh tokens.
     *
     * @param refreshToken The refresh token used to authenticate and retrieve new tokens.
     * @return A `TokenIssueResult` containing the newly issued token pair and the associated account profile.
     */
    fun refresh(refreshToken: String): TokenIssueResult

    /**
     * Retrieves an authenticated principal from the provided access token.
     *
     * This method processes the given access token to extract the associated
     * principal, which contains information about the authenticated user
     * or entity. The extracted principal is returned in the form of a `TokenPrincipal`.
     *
     * @param accessToken The access token used to authenticate and retrieve the associated principal.
     * @return A `TokenPrincipal` representing the authenticated entity derived from the access token.
     */
    fun access(accessToken: String): TokenPrincipal

    /**
     * Logs out a user by revoking the session associated with the provided refresh token.
     *
     * This method invalidates the given refresh token, effectively ending the user's
     * authentication session. Once the operation is successful, the user will no longer
     * be able to use the token to refresh or access the system.
     *
     * @param refreshToken The refresh token identifying the session that needs to be revoked.
     * @return A boolean indicating whether the logout process was successful. Returns true
     *         if the token was successfully revoked, false otherwise.
     */
    fun logout(refreshToken: String): Boolean
}
