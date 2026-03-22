package dev.haomin.resumer.app.auth.service.dto

/**
 * Command object for handling email and password-based login requests.
 *
 * @property email The email address provided by the user for authentication.
 * @property password The plaintext password provided by the user for authentication.
 */
data class EmailPwdLoginCmd(
    val email: String,
    val password: String,
)