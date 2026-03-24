package dev.haomin.resumer.app.module.resume.repo.query

import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import java.time.OffsetDateTime

data class ResumeUpdateQuery(
    val filename: String? = null,
    val content: String? = null,
    val fileHash: String? = null,
    val size: Long? = null,
    val storageEngine: String? = null,
    val storageKey: String? = null,
    val status: ResumeStatus? = null,
    val error: String? = null,
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
