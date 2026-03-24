package dev.haomin.resumer.app.module.session.domain

import java.time.OffsetDateTime
import java.util.UUID

data class ApplicationSession(
    val id: UUID,
    val accountId: UUID,
    val resumeId: UUID?,
    val company: String,
    val position: String,
    val jobDescription: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
