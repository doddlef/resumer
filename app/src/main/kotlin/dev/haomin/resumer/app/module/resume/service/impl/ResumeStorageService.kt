package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.infra.file.FileHashService
import dev.haomin.resumer.app.infra.file.model.FileSource
import dev.haomin.resumer.app.infra.storage.FileEngine
import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.resume.repo.query.ResumeInsertQuery
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ResumeStorageService(
    private val resumeRepo: ResumeRepo,
    private val fileEngine: FileEngine,
    private val hashService: FileHashService,
) {
    /**
     * Calculate the content hash of the file, which can be used to check if the same resume already exists in the system.
     */
    fun calculateContentHash(file: FileSource): String =
        hashService.sha256(file)

    fun findExistingResume(hash: String, accountId: UUID): Resume? =
        // TODO: find resume by content hash + account id
        null

    /**
     * Upload the resume file to the storage engine,
     *
     * @param file the resume file to be uploaded
     * @param accountId the account ID to which the resume belongs, used for organizing files
     * @return the file key that can be used to access the uploaded resume in the storage system
     */
    fun uploadResume(file: FileSource, accountId: UUID): String =
        fileEngine.uploadFile(file, "resumes/$accountId/")

    data class SaveResumeCmd(
        val accountId: UUID,
        val fileName: String,
        val content: String,
        val fileHash: String,
        val size: Long,
        val storageKey: String,
    )

    fun saveResume(cmd: SaveResumeCmd): Resume =
        ResumeInsertQuery(
            accountId = cmd.accountId,
            filename = cmd.fileName,
            content = cmd.content,
            fileHash = cmd.fileHash,
            size = cmd.size,
            storageKey = cmd.storageKey,
            status = ResumeStatus.PENDING,
        )
            .let { resumeRepo.insertAndReturn(it) }
}
