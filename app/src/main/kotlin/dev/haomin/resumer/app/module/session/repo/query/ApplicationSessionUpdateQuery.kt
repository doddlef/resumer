package dev.haomin.resumer.app.module.session.repo.query

import java.time.OffsetDateTime
import java.util.UUID

data class ApplicationSessionUpdateQuery(
    val resumeId: UUID? = null,
    val company: String? = null,
    val position: String? = null,
    val jobDescription: String? = null,
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
