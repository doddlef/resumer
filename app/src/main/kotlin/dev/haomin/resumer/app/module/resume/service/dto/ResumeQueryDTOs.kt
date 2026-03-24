package dev.haomin.resumer.app.module.resume.service.dto

import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.domain.ResumeSuggestion
import java.time.OffsetDateTime
import java.util.UUID

data class ResumeListCmd(
    val accountId: UUID,
)

data class ResumeListResult(
    val resumes: List<ResumeListItem>,
)

data class ResumeListItem(
    val id: UUID,
    val name: String,
    val status: ResumeStatus,
    val score: Int?,
    val createdAt: OffsetDateTime,
)

data class ResumeDetailCmd(
    val resumeId: UUID,
    val accountId: UUID,
)

data class ResumeDetailResult(
    val id: UUID,
    val name: String,
    val status: ResumeStatus,
    val score: Int?,
    val summary: String?,
    val strengths: List<String>,
    val suggestions: List<ResumeSuggestion>,
    val error: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class ResumeStatusPollCmd(
    val resumeId: UUID,
    val accountId: UUID,
)

data class ResumeStatusPollResult(
    val resumeId: UUID,
    val status: ResumeStatus,
    val score: Int?,
    val error: String?,
    val updatedAt: OffsetDateTime,
)
