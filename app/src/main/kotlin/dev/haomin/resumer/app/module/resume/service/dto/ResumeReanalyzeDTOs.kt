package dev.haomin.resumer.app.module.resume.service.dto

import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import java.util.UUID

data class ResumeReanalyzeCmd(
    val resumeId: UUID,
    val accountId: UUID,
)

data class ResumeReanalyzeResult(
    val resumeId: UUID,
    val status: ResumeStatus,
    val traceId: String,
)
