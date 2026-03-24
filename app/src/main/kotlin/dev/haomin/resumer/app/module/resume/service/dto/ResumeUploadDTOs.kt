package dev.haomin.resumer.app.module.resume.service.dto

import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class ResumeUploadCmd(
    val file: MultipartFile,
    val accountId: UUID,
)

data class ResumeUploadResult(
    val duplicate: Boolean,
)