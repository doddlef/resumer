package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.SessionAdviceRepo
import dev.haomin.resumer.app.module.session.service.SessionAdviceReadService
import dev.haomin.resumer.app.module.session.service.dto.SessionAdviceReadCmd
import org.springframework.stereotype.Service

@Service
class SessionAdviceReadServiceImpl(
    private val applicationSessionRepo: ApplicationSessionRepo,
    private val sessionAdviceRepo: SessionAdviceRepo,
) : SessionAdviceReadService {

    override fun latest(cmd: SessionAdviceReadCmd): SessionAdvice {
        val session = applicationSessionRepo.selectById(cmd.sessionId)
            ?: throw NotFoundException("application session not found")
        if (session.accountId != cmd.accountId) {
            throw NotFoundException("application session not found")
        }
        return sessionAdviceRepo.selectLatestBySessionId(session.id)
            ?: throw NotFoundException("session advice not found")
    }
}
