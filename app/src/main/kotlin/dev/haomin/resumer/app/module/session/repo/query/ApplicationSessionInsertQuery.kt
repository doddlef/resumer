package dev.haomin.resumer.app.module.session.repo.query

import dev.haomin.resumer.app.common.id.UUIDGenerator
import java.time.OffsetDateTime
import java.util.UUID

data class ApplicationSessionInsertQuery(
    val id: UUID = UUIDGenerator.next(),
    val accountId: UUID,
    val resumeId: UUID?,
    val company: String,
    val position: String,
    val jobDescription: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
