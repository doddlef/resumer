package dev.haomin.resumer.app.module.resume.repo.query

import dev.haomin.resumer.app.common.id.UUIDGenerator
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import java.time.OffsetDateTime
import java.util.UUID

data class ResumeInsertQuery(
    val id: UUID = UUIDGenerator.next(),
    val accountId: UUID,
    val filename: String,
    val content: String? = null,
    val fileHash: String,
    val size: Long,
    val storageEngine: String,
    val storageKey: String,
    val status: ResumeStatus = ResumeStatus.PENDING,
    val error: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
