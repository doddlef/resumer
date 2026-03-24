package dev.haomin.resumer.app.module.session.repo

import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.repo.query.SessionAdviceInsertQuery
import java.util.UUID

interface SessionAdviceRepo {
    fun selectLatestBySessionId(sessionId: UUID): SessionAdvice?

    fun insertAndReturn(query: SessionAdviceInsertQuery): SessionAdvice
}
