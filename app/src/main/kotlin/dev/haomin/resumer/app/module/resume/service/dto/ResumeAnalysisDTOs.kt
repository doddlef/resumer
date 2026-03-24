package dev.haomin.resumer.app.module.resume.service.dto

import java.util.UUID

data class ResumeAnalyzeCmd(
    val resumeId: UUID,
    val accountId: UUID,
    val traceId: String,
)
