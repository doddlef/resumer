package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.infra.file.model.FileSource
import dev.haomin.resumer.app.module.resume.prop.ResumeProps
import org.springframework.stereotype.Service

@Service
class ResumeValidationService(
    private val props: ResumeProps,
) {
    private val supportTypes = props.supportedContentTypes
        .map { it.lowercase() }
        .toSet()

    /**
     * Validates the uploaded resume file.
     */
    fun validateFile(file: FileSource) {
        if (file.fileName.isNullOrBlank()) {
            throw InvalidParamException("file cannot be empty")
        }

        if (file.size <= 0L) {
            throw InvalidParamException("file is empty")
        }

        if (file.size > props.maxFileSizeBytes) {
            throw InvalidParamException("file is too large")
        }
    }

    /**
     * Validates whether the given content type is supported for resume upload.
     */
    fun validateContentType(contentType: String) {
        val normalized = contentType.trim().lowercase()
        if (normalized.isBlank()) {
            throw InvalidParamException("file content type is required")
        }
        if (normalized !in supportTypes) {
            throw InvalidParamException("unsupported file content type: $contentType")
        }
    }
}
