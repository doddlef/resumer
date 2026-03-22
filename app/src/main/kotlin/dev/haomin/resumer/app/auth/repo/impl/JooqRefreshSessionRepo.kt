package dev.haomin.resumer.app.auth.repo.impl

import dev.haomin.filesheep.jooq.tables.pojos.P_RefreshSessions
import dev.haomin.filesheep.jooq.tables.references.REFRESH_SESSIONS
import dev.haomin.resumer.app.auth.domain.RefreshSession
import dev.haomin.resumer.app.auth.repo.RefreshSessionRepo
import dev.haomin.resumer.app.auth.repo.query.RefreshSessionInsertQuery
import dev.haomin.resumer.app.auth.repo.query.RefreshSessionUpdateQuery
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * jOOQ-based implementation of [RefreshSessionRepo].
 */
@Repository
class JooqRefreshSessionRepo(
    private val dsl: DSLContext,
) : RefreshSessionRepo {

    /**
     * Insert the refresh session row.
     */
    override fun insert(query: RefreshSessionInsertQuery): Int =
        dsl.newRecord(REFRESH_SESSIONS)
            .apply {
                this.id = query.id
                this.accountId = query.accountId
                this.secret = query.secret
                this.prevSecret = query.prevSecret
                this.lastUsedAt = query.lastUsedAt
                this.revokedAt = query.revokedAt
                this.revokeReason = query.revokeReason
                this.createdAt = query.createdAt
                this.updatedAt = query.updatedAt
            }
            .let { dsl.insertInto(REFRESH_SESSIONS).set(it).execute() }

    /**
     * Fetch a refresh session by id and lock it for update.
     */
    override fun selectByIdForUpdate(id: UUID): RefreshSession? =
        dsl.selectFrom(REFRESH_SESSIONS)
            .where(REFRESH_SESSIONS.id.eq(id))
            .forUpdate()
            .fetchOneInto(P_RefreshSessions::class.java)
            ?.toDomain()

    /**
     * Partial update for refresh session row.
     */
    override fun updateById(id: UUID, query: RefreshSessionUpdateQuery): Int =
        dsl.newRecord(REFRESH_SESSIONS)
            .apply {
                query.secret?.let { set(REFRESH_SESSIONS.secret, it) }
                query.prevSecret?.let { set(REFRESH_SESSIONS.prevSecret, it) }
                query.lastUsedAt?.let { set(REFRESH_SESSIONS.lastUsedAt, it) }
                query.revokedAt?.let { set(REFRESH_SESSIONS.revokedAt, it) }
                query.revokeReason?.let { set(REFRESH_SESSIONS.revokeReason, it) }
                set(REFRESH_SESSIONS.updatedAt, query.updatedAt)
            }
            .let { record ->
                if (record.modified()) {
                    dsl.update(REFRESH_SESSIONS).set(record).where(REFRESH_SESSIONS.id.eq(id)).execute()
                } else {
                    0
                }
            }
}

/**
 * jOOQ POJO to domain object mapping.
 */
internal fun P_RefreshSessions.toDomain(): RefreshSession =
    RefreshSession(
        id = requireNotNull(id) { "RefreshSession.id is null" },
        accountId = requireNotNull(accountId) { "RefreshSession.accountId is null" },
        secret = requireNotNull(secret) { "RefreshSession.secret is null" },
        prevSecret = prevSecret,
        lastUsedAt = lastUsedAt,
        revokedAt = revokedAt,
        revokeReason = revokeReason,
        createdAt = requireNotNull(createdAt) { "RefreshSession.createdAt is null" },
        updatedAt = requireNotNull(updatedAt) { "RefreshSession.updatedAt is null" },
    )
