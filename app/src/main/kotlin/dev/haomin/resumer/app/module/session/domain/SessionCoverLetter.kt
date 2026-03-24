package dev.haomin.resumer.app.module.session.domain

import java.time.OffsetDateTime
import java.util.UUID

data class SessionCoverLetter(
    val id: UUID,
    val sessionId: UUID,
    val version: Int,
    val content: String,
    val createdAt: OffsetDateTime,
)
