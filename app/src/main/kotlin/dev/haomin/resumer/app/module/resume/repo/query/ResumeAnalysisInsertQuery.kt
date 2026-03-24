package dev.haomin.resumer.app.module.resume.repo.query

import dev.haomin.resumer.app.common.id.UUIDGenerator
import dev.haomin.resumer.app.module.resume.domain.ResumeSuggestion
import java.time.OffsetDateTime
import java.util.UUID

data class ResumeAnalysisInsertQuery(
    val id: UUID = UUIDGenerator.next(),
    val resumeId: UUID,
    val score: Int,
    val summary: String,
    val strengths: List<String>,
    val suggestions: List<ResumeSuggestion>,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
