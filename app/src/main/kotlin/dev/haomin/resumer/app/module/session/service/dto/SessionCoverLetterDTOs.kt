package dev.haomin.resumer.app.module.session.service.dto

import java.util.UUID

data class SessionCoverLetterGenerateCmd(
    val sessionId: UUID,
    val accountId: UUID,
    val traceId: String,
)

data class SessionCoverLetterGetCmd(
    val sessionId: UUID,
    val accountId: UUID,
    val version: Int? = null,
)
