package dev.haomin.resumer.app.module.resume.service.dto

import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime
import java.util.UUID

data class ResumeUploadCmd(
    val file: MultipartFile,
    val accountId: UUID,
)

data class ResumeUploadResult(
    val resumeId: UUID,
    val name: String,
    val status: ResumeStatus,
    val duplicate: Boolean,
    val createdAt: OffsetDateTime,
)
