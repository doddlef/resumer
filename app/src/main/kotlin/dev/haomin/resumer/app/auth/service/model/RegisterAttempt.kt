package dev.haomin.resumer.app.auth.service.model

import java.time.OffsetDateTime

data class RegisterAttempt(
    val attemptId: String,
    val email: String,
    val codeHash: String?,
    val verifiedAt: OffsetDateTime? = null,
    val tryCount: Int = 0,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
