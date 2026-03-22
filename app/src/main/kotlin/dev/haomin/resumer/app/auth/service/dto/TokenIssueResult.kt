package dev.haomin.resumer.app.auth.service.dto

import dev.haomin.resumer.app.auth.domain.AccountProfile
import dev.haomin.resumer.app.auth.domain.TokenPair

/**
 * Represents the result of a token issuance operation.
 *
 * The `TokenIssueResult` class encapsulates the outcome of issuing authentication tokens
 * and includes two key pieces of data:
 * 1. A pair of authentication tokens (`TokenPair`) consisting of an access token and a refresh token.
 * 2. A read-only profile view of the associated account (`AccountProfile`), which provides
 *    basic account information without revealing sensitive details.
 *
 * This class is used in authentication-related workflows, such as login and token refresh processes.
 *
 * @property tokenPair The pair of access and refresh tokens issued as part of the authentication process.
 * @property account The read-only profile representation of the authenticated account.
 */
data class TokenIssueResult(
    val tokenPair: TokenPair,
    val account: AccountProfile,
)
