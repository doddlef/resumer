package dev.haomin.resumer.app.module.session.repo.query

import dev.haomin.resumer.app.common.id.UUIDGenerator
import java.time.OffsetDateTime
import java.util.UUID

data class SessionCoverLetterInsertQuery(
    val id: UUID = UUIDGenerator.next(),
    val sessionId: UUID,
    val version: Int,
    val content: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
