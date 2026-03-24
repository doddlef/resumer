package dev.haomin.resumer.app.module.session.mq

import dev.haomin.resumer.app.module.session.domain.TaskStatus
import dev.haomin.resumer.app.module.session.mq.model.AdviceJobType
import dev.haomin.resumer.app.module.session.mq.model.SessionAdviceJobPayload
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.query.ApplicationSessionUpdateQuery
import dev.haomin.resumer.app.module.session.service.PositionAdviceService
import dev.haomin.resumer.app.module.session.service.ResumeFitAdviceService
import dev.haomin.resumer.app.module.session.service.dto.PositionAdviceGenerateCmd
import dev.haomin.resumer.app.module.session.service.dto.ResumeFitAdviceGenerateCmd
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface SessionAdviceJobProcessor {
    fun process(payload: SessionAdviceJobPayload)
}

@Service
class SessionAdviceJobProcessorImpl(
    private val applicationSessionRepo: ApplicationSessionRepo,
    private val positionAdviceService: PositionAdviceService,
    private val resumeFitAdviceService: ResumeFitAdviceService,
) : SessionAdviceJobProcessor {
    private val logger = LoggerFactory.getLogger(SessionAdviceJobProcessorImpl::class.java)

    override fun process(payload: SessionAdviceJobPayload) {
        when (payload.jobType) {
            AdviceJobType.POSITION_ADVICE -> processPositionAdvice(payload)
            AdviceJobType.RESUME_FIT -> processResumeFit(payload)
        }
    }

    private fun processPositionAdvice(payload: SessionAdviceJobPayload) {
        applicationSessionRepo.updateById(
            payload.sessionId,
            ApplicationSessionUpdateQuery(
                positionAdviceStatus = TaskStatus.PROCESSING,
                clearPositionAdviceError = true,
            ),
        )

        runCatching {
            positionAdviceService.generate(
                PositionAdviceGenerateCmd(
                    sessionId = payload.sessionId,
                    accountId = payload.accountId,
                    traceId = "position-advice-${payload.sessionId}",
                ),
            )
        }.onSuccess {
            applicationSessionRepo.updateById(
                payload.sessionId,
                ApplicationSessionUpdateQuery(
                    positionAdviceStatus = TaskStatus.COMPLETED,
                    clearPositionAdviceError = true,
                ),
            )
        }.onFailure { error ->
            applicationSessionRepo.updateById(
                payload.sessionId,
                ApplicationSessionUpdateQuery(
                    positionAdviceStatus = TaskStatus.FAILED,
                    positionAdviceError = sanitizeError(error.message),
                ),
            )
            logger.error(
                "Position advice job failed: sessionId={}, accountId={}",
                payload.sessionId,
                payload.accountId,
                error,
            )
            throw error
        }
    }

    private fun processResumeFit(payload: SessionAdviceJobPayload) {
        applicationSessionRepo.updateById(
            payload.sessionId,
            ApplicationSessionUpdateQuery(
                resumeFitStatus = TaskStatus.PROCESSING,
                clearResumeFitError = true,
            ),
        )
        runCatching {
            resumeFitAdviceService.generate(
                ResumeFitAdviceGenerateCmd(
                    sessionId = payload.sessionId,
                    accountId = payload.accountId,
                    traceId = "resume-fit-${payload.sessionId}",
                ),
            )
        }.onSuccess {
            applicationSessionRepo.updateById(
                payload.sessionId,
                ApplicationSessionUpdateQuery(
                    resumeFitStatus = TaskStatus.COMPLETED,
                    clearResumeFitError = true,
                ),
            )
        }.onFailure { error ->
            applicationSessionRepo.updateById(
                payload.sessionId,
                ApplicationSessionUpdateQuery(
                    resumeFitStatus = TaskStatus.FAILED,
                    resumeFitError = sanitizeError(error.message),
                ),
            )
            logger.error(
                "Resume fit job failed: sessionId={}, accountId={}",
                payload.sessionId,
                payload.accountId,
                error,
            )
            throw error
        }
    }

    private fun sanitizeError(message: String?): String =
        message
            ?.replace('\n', ' ')
            ?.replace('\r', ' ')
            ?.trim()
            ?.take(500)
            .orEmpty()
            .ifBlank { "unknown error" }
}
