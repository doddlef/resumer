package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.common.id.UUIDGenerator
import dev.haomin.resumer.app.module.session.domain.TaskStatus
import dev.haomin.resumer.app.module.session.mq.SessionAdvicePublisher
import dev.haomin.resumer.app.module.session.mq.model.AdviceJobType
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.query.ApplicationSessionUpdateQuery
import dev.haomin.resumer.app.module.session.service.SessionAdviceTaskService
import dev.haomin.resumer.app.module.session.service.dto.QueueSessionAdviceTaskCmd
import dev.haomin.resumer.app.module.session.service.dto.QueueSessionAdviceTaskResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SessionAdviceTaskServiceImpl(
    private val applicationSessionRepo: ApplicationSessionRepo,
    private val publisher: SessionAdvicePublisher,
) : SessionAdviceTaskService {

    private companion object {
        private val logger = LoggerFactory.getLogger(SessionAdviceTaskServiceImpl::class.java)
    }

    override fun queue(cmd: QueueSessionAdviceTaskCmd): QueueSessionAdviceTaskResult {
        val session = applicationSessionRepo.selectById(cmd.sessionId)
            ?: throw NotFoundException("application session not found")
        if (session.accountId != cmd.accountId) {
            throw NotFoundException("application session not found")
        }

        when (cmd.jobType) {
            AdviceJobType.POSITION_ADVICE -> {
                applicationSessionRepo.updateById(
                    cmd.sessionId,
                    ApplicationSessionUpdateQuery(
                        positionAdviceStatus = TaskStatus.PENDING,
                        clearPositionAdviceError = true,
                    ),
                )
            }
            AdviceJobType.RESUME_FIT -> {
                applicationSessionRepo.updateById(
                    cmd.sessionId,
                    ApplicationSessionUpdateQuery(
                        resumeFitStatus = TaskStatus.PENDING,
                        clearResumeFitError = true,
                    ),
                )
            }
        }

        val traceId = UUIDGenerator.next().toString()
        runCatching {
            publisher.publish(
                sessionId = cmd.sessionId,
                accountId = cmd.accountId,
                jobType = cmd.jobType,
                traceId = traceId,
            )
        }.onFailure { error ->
            when (cmd.jobType) {
                AdviceJobType.POSITION_ADVICE -> {
                    applicationSessionRepo.updateById(
                        cmd.sessionId,
                        ApplicationSessionUpdateQuery(
                            positionAdviceStatus = TaskStatus.FAILED,
                            positionAdviceError = "failed to queue position advice: ${error.message ?: "unknown"}",
                        ),
                    )
                }
                AdviceJobType.RESUME_FIT -> {
                    applicationSessionRepo.updateById(
                        cmd.sessionId,
                        ApplicationSessionUpdateQuery(
                            resumeFitStatus = TaskStatus.FAILED,
                            resumeFitError = "failed to queue resume fit advice: ${error.message ?: "unknown"}",
                        ),
                    )
                }
            }

            logger.error(
                "Failed to queue session advice job: sessionId={}, accountId={}, jobType={}, traceId={}",
                cmd.sessionId,
                cmd.accountId,
                cmd.jobType,
                traceId,
                error,
            )
            throw AppException(message = "failed to queue session advice task")
        }

        return QueueSessionAdviceTaskResult(
            sessionId = cmd.sessionId,
            jobType = cmd.jobType,
            status = TaskStatus.PENDING,
            traceId = traceId,
        )
    }
}
