package dev.haomin.resumer.app.module.resume.domain

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents the processing status of a resume.
 *
 * Enum Constants:
 * - PENDING: The resume is uploaded and waiting for analysis.
 * - ANALYZING: The resume is currently being analyzed.
 * - COMPLETED: Analysis has completed and results are available.
 * - FAILED: Analysis has failed and error details may be available.
 *
 * Companion Object Functions:
 * - fromString(value: String): Parses a string to match a [ResumeStatus] value, ignoring case.
 */
enum class ResumeStatus {
    PENDING,
    ANALYZING,
    COMPLETED,
    FAILED;

    companion object {
        fun fromString(value: String): ResumeStatus =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid ResumeStatus: $value")
    }
}

/**
 * Represents an uploaded resume and its current processing state.
 *
 * @property id Unique identifier of the resume record.
 * @property accountId Unique identifier of the account that owns this resume.
 * @property filename Original filename uploaded by the user.
 * @property content Extracted text content from the file; nullable when not yet parsed.
 * @property fileHash Hash of the original file content for deduplication/integrity checks.
 * @property size File size in bytes.
 * @property storageEngine Storage provider identifier (for example, local or s3).
 * @property storageKey Object key/path in the storage engine.
 * @property status Current resume processing status.
 * @property error Optional failure reason when processing fails.
 * @property createdAt Timestamp when the resume record was created.
 * @property updatedAt Timestamp when the resume record was last updated.
 */
data class Resume(
    val id: UUID,
    val accountId: UUID,
    val filename: String,
    val content: String?,
    val fileHash: String,
    val size: Long,
    val storageEngine: String,
    val storageKey: String,
    val status: ResumeStatus,
    val error: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
