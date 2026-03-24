package dev.haomin.resumer.app.module.session.service.dto

import java.util.UUID

data class ResumeFitAdviceGenerateCmd(
    val sessionId: UUID,
    val accountId: UUID,
    val traceId: String,
)
