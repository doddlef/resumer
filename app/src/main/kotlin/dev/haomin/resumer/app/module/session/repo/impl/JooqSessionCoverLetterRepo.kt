package dev.haomin.resumer.app.module.session.repo.impl

import dev.haomin.filesheep.jooq.tables.pojos.P_SessionCoverLetters
import dev.haomin.filesheep.jooq.tables.references.SESSION_COVER_LETTERS
import dev.haomin.resumer.app.module.session.domain.SessionCoverLetter
import dev.haomin.resumer.app.module.session.repo.SessionCoverLetterRepo
import dev.haomin.resumer.app.module.session.repo.query.SessionCoverLetterInsertQuery
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.jooq.impl.DSL
import java.util.UUID

@Repository
class JooqSessionCoverLetterRepo(
    private val dsl: DSLContext,
) : SessionCoverLetterRepo {

    override fun selectLatestBySessionId(sessionId: UUID): SessionCoverLetter? =
        dsl.selectFrom(SESSION_COVER_LETTERS)
            .where(SESSION_COVER_LETTERS.sessionId.eq(sessionId))
            .orderBy(SESSION_COVER_LETTERS.version.desc())
            .limit(1)
            .fetchOneInto(P_SessionCoverLetters::class.java)
            ?.toDomain()

    override fun selectBySessionIdAndVersion(sessionId: UUID, version: Int): SessionCoverLetter? =
        dsl.selectFrom(SESSION_COVER_LETTERS)
            .where(SESSION_COVER_LETTERS.sessionId.eq(sessionId))
            .and(SESSION_COVER_LETTERS.version.eq(version))
            .fetchOneInto(P_SessionCoverLetters::class.java)
            ?.toDomain()

    override fun selectMaxVersionBySessionId(sessionId: UUID): Int? =
        dsl.select(DSL.max(SESSION_COVER_LETTERS.version))
            .from(SESSION_COVER_LETTERS)
            .where(SESSION_COVER_LETTERS.sessionId.eq(sessionId))
            .fetchOne(0, Int::class.java)

    override fun insertAndReturn(query: SessionCoverLetterInsertQuery): SessionCoverLetter =
        dsl.newRecord(SESSION_COVER_LETTERS)
            .apply {
                this.id = query.id
                this.sessionId = query.sessionId
                this.version = query.version
                this.content = query.content
                this.createdAt = query.createdAt
            }
            .let { dsl.insertInto(SESSION_COVER_LETTERS).set(it).returning().fetchOneInto(P_SessionCoverLetters::class.java) }
            ?.toDomain()
            ?: throw IllegalStateException("Failed to insert session cover letter and return the record")
}

internal fun P_SessionCoverLetters.toDomain(): SessionCoverLetter =
    SessionCoverLetter(
        id = requireNotNull(id) { "SessionCoverLetter.id is null" },
        sessionId = requireNotNull(sessionId) { "SessionCoverLetter.sessionId is null" },
        version = requireNotNull(version) { "SessionCoverLetter.version is null" },
        content = requireNotNull(content) { "SessionCoverLetter.content is null" },
        createdAt = requireNotNull(createdAt) { "SessionCoverLetter.createdAt is null" },
    )
