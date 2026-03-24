package dev.haomin.resumer.app.module.session.repo.impl

import dev.haomin.filesheep.jooq.enums.E_SessionTaskStatus
import dev.haomin.filesheep.jooq.tables.pojos.P_ApplicationSessions
import dev.haomin.filesheep.jooq.tables.references.APPLICATION_SESSIONS
import dev.haomin.resumer.app.module.session.domain.ApplicationSession
import dev.haomin.resumer.app.module.session.domain.TaskStatus
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.query.ApplicationSessionInsertQuery
import dev.haomin.resumer.app.module.session.repo.query.ApplicationSessionUpdateQuery
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqApplicationSessionRepo(
    private val dsl: DSLContext,
) : ApplicationSessionRepo {

    override fun selectById(id: UUID): ApplicationSession? =
        dsl.selectFrom(APPLICATION_SESSIONS)
            .where(APPLICATION_SESSIONS.id.eq(id))
            .fetchOneInto(P_ApplicationSessions::class.java)
            ?.toDomain()

    override fun selectByAccountId(accountId: UUID): List<ApplicationSession> =
        dsl.selectFrom(APPLICATION_SESSIONS)
            .where(APPLICATION_SESSIONS.accountId.eq(accountId))
            .orderBy(APPLICATION_SESSIONS.createdAt.desc())
            .fetchInto(P_ApplicationSessions::class.java)
            .map { it.toDomain() }

    override fun insertAndReturn(query: ApplicationSessionInsertQuery): ApplicationSession =
        dsl.newRecord(APPLICATION_SESSIONS)
            .apply {
                this.id = query.id
                this.accountId = query.accountId
                this.resumeId = query.resumeId
                this.company = query.company
                this.position = query.position
                this.jobDescription = query.jobDescription
                this.createdAt = query.createdAt
                this.updatedAt = query.updatedAt
                this.positionAdviceStatus = TaskStatus.PENDING.toJooq()
                this.resumeFitStatus = TaskStatus.PENDING.toJooq()
            }
            .let { dsl.insertInto(APPLICATION_SESSIONS).set(it).returning().fetchOneInto(P_ApplicationSessions::class.java) }
            ?.toDomain()
            ?: throw IllegalStateException("Failed to insert application session and return the record")

    override fun updateById(id: UUID, query: ApplicationSessionUpdateQuery): Int =
        dsl.newRecord(APPLICATION_SESSIONS)
            .apply {
                query.resumeId?.let { set(APPLICATION_SESSIONS.resumeId, it) }
                query.company?.let { set(APPLICATION_SESSIONS.company, it) }
                query.position?.let { set(APPLICATION_SESSIONS.position, it) }
                query.jobDescription?.let { set(APPLICATION_SESSIONS.jobDescription, it) }
                query.positionAdviceStatus?.let { set(APPLICATION_SESSIONS.positionAdviceStatus, it.toJooq()) }
                if (query.clearPositionAdviceError) {
                    set(APPLICATION_SESSIONS.positionAdviceError, null as String?)
                } else {
                    query.positionAdviceError?.let { set(APPLICATION_SESSIONS.positionAdviceError, it) }
                }
                query.resumeFitStatus?.let { set(APPLICATION_SESSIONS.resumeFitStatus, it.toJooq()) }
                if (query.clearResumeFitError) {
                    set(APPLICATION_SESSIONS.resumeFitError, null as String?)
                } else {
                    query.resumeFitError?.let { set(APPLICATION_SESSIONS.resumeFitError, it) }
                }
                set(APPLICATION_SESSIONS.updatedAt, query.updatedAt)
            }
            .let { record ->
                if (record.modified()) {
                    dsl.update(APPLICATION_SESSIONS).set(record).where(APPLICATION_SESSIONS.id.eq(id)).execute()
                } else {
                    0
                }
            }
}

internal fun P_ApplicationSessions.toDomain(): ApplicationSession =
    ApplicationSession(
        id = requireNotNull(id) { "ApplicationSession.id is null" },
        accountId = requireNotNull(accountId) { "ApplicationSession.accountId is null" },
        resumeId = resumeId,
        company = requireNotNull(company) { "ApplicationSession.company is null" },
        position = requireNotNull(position) { "ApplicationSession.position is null" },
        jobDescription = requireNotNull(jobDescription) { "ApplicationSession.jobDescription is null" },
        createdAt = requireNotNull(createdAt) { "ApplicationSession.createdAt is null" },
        updatedAt = requireNotNull(updatedAt) { "ApplicationSession.updatedAt is null" },
        positionAdviceStatus = requireNotNull(positionAdviceStatus) { "ApplicationSession.positionAdviceStatus is null" }.toDomain(),
        positionAdviceError = positionAdviceError,
        resumeFitStatus = requireNotNull(resumeFitStatus) { "ApplicationSession.resumeFitStatus is null" }.toDomain(),
        resumeFitError = resumeFitError,
    )

internal fun TaskStatus.toJooq(): E_SessionTaskStatus = E_SessionTaskStatus.valueOf(name)

internal fun E_SessionTaskStatus.toDomain(): TaskStatus = TaskStatus.fromString(name)
