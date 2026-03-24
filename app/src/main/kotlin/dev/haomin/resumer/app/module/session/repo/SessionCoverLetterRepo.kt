package dev.haomin.resumer.app.module.session.repo

import dev.haomin.resumer.app.module.session.domain.SessionCoverLetter
import dev.haomin.resumer.app.module.session.repo.query.SessionCoverLetterInsertQuery
import java.util.UUID

interface SessionCoverLetterRepo {
    fun selectLatestBySessionId(sessionId: UUID): SessionCoverLetter?

    fun selectBySessionIdAndVersion(sessionId: UUID, version: Int): SessionCoverLetter?

    fun selectMaxVersionBySessionId(sessionId: UUID): Int?

    fun insertAndReturn(query: SessionCoverLetterInsertQuery): SessionCoverLetter
}
