package dev.haomin.resumer.app.module.session.service.dto

import dev.haomin.resumer.app.module.session.domain.TaskStatus
import java.time.OffsetDateTime
import java.util.UUID

data class ApplicationSessionCreateCmd(
    val accountId: UUID,
    val resumeId: UUID?,
    val company: String,
    val position: String,
    val jobDescription: String,
)

data class ApplicationSessionCreateResult(
    val id: UUID,
    val resumeId: UUID?,
    val company: String,
    val position: String,
    val jobDescription: String,
    val createdAt: OffsetDateTime,
)

data class ApplicationSessionListCmd(
    val accountId: UUID,
)

data class ApplicationSessionListResult(
    val sessions: List<ApplicationSessionListItem>,
)

data class ApplicationSessionListItem(
    val id: UUID,
    val resumeId: UUID?,
    val company: String,
    val position: String,
    val positionAdviceStatus: TaskStatus,
    val resumeFitStatus: TaskStatus,
    val createdAt: OffsetDateTime,
)

data class ApplicationSessionDetailCmd(
    val sessionId: UUID,
    val accountId: UUID,
)

data class ApplicationSessionDetailResult(
    val id: UUID,
    val resumeId: UUID?,
    val company: String,
    val position: String,
    val jobDescription: String,
    val positionAdviceStatus: TaskStatus,
    val positionAdviceError: String?,
    val resumeFitStatus: TaskStatus,
    val resumeFitError: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
