package dev.haomin.resumer.app.auth.service.dto

import java.time.OffsetDateTime
import java.util.UUID

data class SendVerificationCmd(
    val email: String,
)

data class VerifyCodeCmd(
    val attemptId: String,
    val code: String,
)

data class CompleteRegistrationCmd(
    val attemptId: String,
    val nickname: String,
    val password: String,
)

data class ResendVerificationCmd(
    val attemptId: String,
)

data class SendVerificationResult(
    val attemptId: String,
    val expiresInSeconds: Long,
)

data class VerifyCodeResult(
    val verified: Boolean,
    val verifiedAt: OffsetDateTime?,
)

data class CompleteRegistrationResult(
    val accountId: UUID,
    val email: String,
    val nickname: String,
)
