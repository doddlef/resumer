package dev.haomin.resumer.app.module.session.repo.query

import dev.haomin.resumer.app.module.session.domain.TaskStatus
import java.time.OffsetDateTime
import java.util.UUID

data class ApplicationSessionUpdateQuery(
    val resumeId: UUID? = null,
    val company: String? = null,
    val position: String? = null,
    val jobDescription: String? = null,
    val positionAdviceStatus: TaskStatus? = null,
    val positionAdviceError: String? = null,
    val clearPositionAdviceError: Boolean = false,
    val resumeFitStatus: TaskStatus? = null,
    val resumeFitError: String? = null,
    val clearResumeFitError: Boolean = false,
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
