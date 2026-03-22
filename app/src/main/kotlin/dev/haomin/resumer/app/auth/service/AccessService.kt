package dev.haomin.resumer.app.auth.service

import dev.haomin.resumer.app.auth.domain.Account
import dev.haomin.resumer.app.auth.domain.Token
import dev.haomin.resumer.app.auth.principal.TokenPrincipal

/**
 * Provides functionality for issuing and parsing access tokens.
 *
 * This service is responsible for handling access tokens as part of
 * an authentication and authorization system. It allows for the generation
 * of access tokens for accounts and the parsing of tokens to extract
 * associated principals.
 */
interface AccessService {

    /**
     * Issues an access token for the specified account.
     *
     **/
    fun issue(account: Account): Token

    /**
     * Parses the provided access token to extract the associated authenticated principal.
     *
     * This method validates and processes the given access token, deriving a `TokenPrincipal`
     * that contains information about the authenticated user or entity.
     *
     * @param accessToken The access token to be parsed. This token is expected to be a valid
     *                    representation of an authenticated user's session.
     * @return A `TokenPrincipal` representing the authenticated entity associated with the access token.
     *         It contains details such as the principal's unique ID, email, status, and role.
     */
    fun parse(accessToken: String): TokenPrincipal
}
