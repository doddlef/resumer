package dev.haomin.resumer.app.module.session.repo.query

import dev.haomin.resumer.app.common.id.UUIDGenerator
import dev.haomin.resumer.app.module.session.domain.RewriteSuggestion
import java.time.OffsetDateTime
import java.util.UUID

data class SessionAdviceInsertQuery(
    val id: UUID = UUIDGenerator.next(),
    val sessionId: UUID,
    val positionSummary: String,
    val keyRequirements: List<String>,
    val likelyInterviewFocus: List<String>,
    val redFlags: List<String>,
    val fitScore: Int,
    val gaps: List<String>,
    val rewriteSuggestions: List<RewriteSuggestion>,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
