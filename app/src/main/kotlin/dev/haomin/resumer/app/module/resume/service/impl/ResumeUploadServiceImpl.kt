package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.common.id.UUIDGenerator
import dev.haomin.resumer.app.infra.file.model.WrappedFileSource
import dev.haomin.resumer.app.module.resume.mq.ResumeAnalyzePublisher
import dev.haomin.resumer.app.module.resume.service.ResumeUploadService
import dev.haomin.resumer.app.module.resume.service.dto.ResumeUploadCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeUploadResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ResumeUploadServiceImpl(
    private val parseService: ResumeParseService,
    private val storageService: ResumeStorageService,
    private val validateService: ResumeValidationService,
    private val analysisPublisher: ResumeAnalyzePublisher,
): ResumeUploadService {

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ResumeUploadServiceImpl::class.java)
    }

    override fun uploadAndAnalyze(cmd: ResumeUploadCmd): ResumeUploadResult {
        val accountId = cmd.accountId
        val file = WrappedFileSource(cmd.file)

        // 1. validate file
        validateService.validateFile(file)
        logger.info("Receive file upload request: {}, {} bytes", file.fileName, file.size)

        // 2. validate content type
        val contentType = parseService.detectContentType(file)
        validateService.validateContentType(contentType)

        // 3. check resume does not exist based on content hash
        val hash = storageService.calculateContentHash(file)
        storageService.findExistingResume(hash, accountId)?.let {
            logger.info("Resume already exists for account {}, hash {}, resumeId {}", accountId, hash, it.id)
            return ResumeUploadResult(
                duplicate = true,
            )
        }

        // 4. parse file content
        val content = parseService.parseResume(file)
        if (content.isBlank()) {
            logger.info("Resume content is empty after parsing for account {}, hash {}", accountId, hash)
            throw IllegalArgumentException("Resume content is empty after parsing")
        }

        // 5. save file to engine
        val storageKey = storageService.uploadResume(file, accountId)
        logger.info("Resume uploaded to storage: {}", storageKey)

        // 6. save resume metadata to db
        val resume = ResumeStorageService.SaveResumeCmd(
            accountId = accountId,
            fileName = file.fileName ?: "my_resume",
            content = content,
            fileHash = hash,
            size = file.size,
            storageKey = storageKey,
        )
            .let { storageService.saveResume(it) }

        // 7. public event
        val traceId = UUIDGenerator.next().toString()
        analysisPublisher.publishResumeAnalyze(resume.id, resume.accountId, traceId)
        logger.info("Published resume analyze event for resumeId {}, accountId {}, traceId {}", resume.id, resume.accountId, traceId)

        // 8. return result
        return ResumeUploadResult(
            duplicate = false,
        )
    }
}