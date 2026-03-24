package dev.haomin.resumer.app.module.resume.domain

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents a resume analysis result record.
 *
 * @property id Unique identifier of this analysis record.
 * @property resumeId Unique identifier of the resume this analysis belongs to.
 * @property score Overall score of the resume (0-100).
 * @property summary High-level summary/comment for the resume.
 * @property strengthsJson JSON text representing strengths list.
 * @property suggestionsJson JSON text representing suggestions list.
 * @property createdAt Timestamp when this analysis record was created.
 */
data class ResumeAnalysis(
    val id: UUID,
    val resumeId: UUID,
    val score: Int,
    val summary: String,
    val strengths: List<String>,
    val suggestions: List<ResumeSuggestion>,
    val createdAt: OffsetDateTime,
)

data class ResumeSuggestion(
    val category: String,
    val priority: String,
    val issue: String,
    val recommendation: String,
)
