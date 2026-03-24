package dev.haomin.resumer.app.module.session.domain

import java.time.OffsetDateTime
import java.util.UUID

data class SessionAdvice(
    val id: UUID,
    val sessionId: UUID,
    val positionSummary: String,
    val keyRequirements: List<String>,
    val likelyInterviewFocus: List<String>,
    val redFlags: List<String>,
    val fitScore: Int,
    val gaps: List<String>,
    val rewriteSuggestions: List<RewriteSuggestion>,
    val createdAt: OffsetDateTime,
)

data class RewriteSuggestion(
    val section: String,
    val priority: String,
    val issue: String,
    val recommendation: String,
    val example: String? = null,
)
