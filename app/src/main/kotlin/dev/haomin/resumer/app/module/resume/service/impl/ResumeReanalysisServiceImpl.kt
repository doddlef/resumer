package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.common.id.UUIDGenerator
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.mq.ResumeAnalyzePublisher
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.resume.repo.query.ResumeUpdateQuery
import dev.haomin.resumer.app.module.resume.service.ResumeReanalysisService
import dev.haomin.resumer.app.module.resume.service.dto.ResumeReanalyzeCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeReanalyzeResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ResumeReanalysisServiceImpl(
    private val resumeRepo: ResumeRepo,
    private val analysisPublisher: ResumeAnalyzePublisher,
) : ResumeReanalysisService {

    private companion object {
        private val logger = LoggerFactory.getLogger(ResumeReanalysisServiceImpl::class.java)
    }

    override fun reanalyze(cmd: ResumeReanalyzeCmd): ResumeReanalyzeResult {
        val resume = resumeRepo.selectById(cmd.resumeId)
            ?: throw NotFoundException("resume not found")

        // Ownership check: do not allow re-analysis for other users' resumes.
        if (resume.accountId != cmd.accountId) {
            throw NotFoundException("resume not found")
        }

        if (resume.content.isNullOrBlank()) {
            throw InvalidParamException("resume content is empty")
        }

        resumeRepo.updateById(
            resume.id,
            ResumeUpdateQuery(
                status = ResumeStatus.PENDING,
                clearError = true,
            ),
        )

        val traceId = UUIDGenerator.next().toString()
        runCatching {
            analysisPublisher.publishResumeAnalyze(resume.id, resume.accountId, traceId)
        }.onFailure { error ->
            val errorMessage = "failed to publish reanalysis task: ${error.message ?: "unknown"}"
            resumeRepo.updateById(
                resume.id,
                ResumeUpdateQuery(
                    status = ResumeStatus.FAILED,
                    error = errorMessage,
                ),
            )
            logger.error(
                "Failed to publish resume reanalysis event: resumeId={}, accountId={}, traceId={}",
                resume.id,
                resume.accountId,
                traceId,
                error,
            )
            throw AppException(message = "failed to queue resume reanalysis")
        }

        logger.info(
            "Published resume reanalysis event: resumeId={}, accountId={}, traceId={}",
            resume.id,
            resume.accountId,
            traceId,
        )

        return ResumeReanalyzeResult(
            resumeId = resume.id,
            status = ResumeStatus.PENDING,
            traceId = traceId,
        )
    }
}
