package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.session.domain.ApplicationSession
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.query.ApplicationSessionInsertQuery
import dev.haomin.resumer.app.module.session.service.ApplicationSessionService
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionCreateCmd
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionCreateResult
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionDetailCmd
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionDetailResult
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionListCmd
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionListItem
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionListResult
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ApplicationSessionServiceImpl(
    private val applicationSessionRepo: ApplicationSessionRepo,
    private val resumeRepo: ResumeRepo,
) : ApplicationSessionService {

    override fun create(cmd: ApplicationSessionCreateCmd): ApplicationSessionCreateResult {
        val resumeId = cmd.resumeId
        if (resumeId != null) {
            val resume = resumeRepo.selectById(resumeId)
                ?: throw NotFoundException("resume not found")
            if (resume.accountId != cmd.accountId) {
                throw NotFoundException("resume not found")
            }
        }

        val session = applicationSessionRepo.insertAndReturn(
            ApplicationSessionInsertQuery(
                accountId = cmd.accountId,
                resumeId = resumeId,
                company = cmd.company.trim(),
                position = cmd.position.trim(),
                jobDescription = cmd.jobDescription.trim(),
            ),
        )
        return ApplicationSessionCreateResult(
            id = session.id,
            resumeId = session.resumeId,
            company = session.company,
            position = session.position,
            jobDescription = session.jobDescription,
            createdAt = session.createdAt,
        )
    }

    override fun list(cmd: ApplicationSessionListCmd): ApplicationSessionListResult {
        val sessions = applicationSessionRepo.selectByAccountId(cmd.accountId)
        return ApplicationSessionListResult(
            sessions = sessions.map {
                ApplicationSessionListItem(
                    id = it.id,
                    resumeId = it.resumeId,
                    company = it.company,
                    position = it.position,
                    positionAdviceStatus = it.positionAdviceStatus,
                    resumeFitStatus = it.resumeFitStatus,
                    createdAt = it.createdAt,
                )
            },
        )
    }

    override fun detail(cmd: ApplicationSessionDetailCmd): ApplicationSessionDetailResult {
        val session = requireOwnedSession(cmd.sessionId, cmd.accountId)
        return ApplicationSessionDetailResult(
            id = session.id,
            resumeId = session.resumeId,
            company = session.company,
            position = session.position,
            jobDescription = session.jobDescription,
            positionAdviceStatus = session.positionAdviceStatus,
            positionAdviceError = session.positionAdviceError,
            resumeFitStatus = session.resumeFitStatus,
            resumeFitError = session.resumeFitError,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
        )
    }

    private fun requireOwnedSession(sessionId: UUID, accountId: UUID): ApplicationSession {
        val session = applicationSessionRepo.selectById(sessionId)
            ?: throw NotFoundException("application session not found")
        if (session.accountId != accountId) {
            throw NotFoundException("application session not found")
        }
        return session
    }
}
