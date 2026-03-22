package dev.haomin.resumer.app.auth.api.dto

data class AuthLoginRequest(
    val email: String,
    val password: String,
)
