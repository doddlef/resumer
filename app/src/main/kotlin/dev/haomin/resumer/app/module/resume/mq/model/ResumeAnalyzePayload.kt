package dev.haomin.resumer.app.module.resume.mq.model

import java.util.UUID

data class ResumeAnalyzePayload(
    val resumeId: UUID,
    val accountId: UUID,
)
