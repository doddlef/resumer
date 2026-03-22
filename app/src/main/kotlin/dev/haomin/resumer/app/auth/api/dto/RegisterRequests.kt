package dev.haomin.resumer.app.auth.api.dto

data class RegisterStartRequest(
    val email: String,
)

data class RegisterVerifyRequest(
    val attemptId: String,
    val code: String,
)

data class RegisterConfirmRequest(
    val attemptId: String,
    val nickname: String,
    val password: String,
)

data class RegisterResendRequest(
    val attemptId: String,
)
