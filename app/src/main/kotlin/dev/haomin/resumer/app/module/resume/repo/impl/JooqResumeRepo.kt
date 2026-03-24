package dev.haomin.resumer.app.module.resume.repo.impl

import dev.haomin.filesheep.jooq.enums.E_ResumeStatus
import dev.haomin.filesheep.jooq.tables.pojos.P_Resumes
import dev.haomin.filesheep.jooq.tables.references.RESUMES
import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.resume.repo.query.ResumeInsertQuery
import dev.haomin.resumer.app.module.resume.repo.query.ResumeUpdateQuery
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * jOOQ-based implementation of [ResumeRepo].
 */
@Repository
class JooqResumeRepo(
    private val dsl: DSLContext,
) : ResumeRepo {

    /**
     * Fetch resume by id.
     */
    override fun selectById(id: UUID): Resume? =
        dsl.selectFrom(RESUMES)
            .where(RESUMES.id.eq(id))
            .fetchOneInto(P_Resumes::class.java)
            ?.toDomain()

    /**
     * Insert resume and return inserted row.
     */
    override fun insertAndReturn(query: ResumeInsertQuery): Resume =
        dsl.newRecord(RESUMES)
            .apply {
                this.id = query.id
                this.accountId = query.accountId
                this.filename = query.filename
                this.content = query.content
                this.fileHash = query.fileHash
                this.size = query.size
                this.storageEngine = query.storageEngine
                this.storageKey = query.storageKey
                this.status = query.status.toJooq()
                this.error = query.error
                this.createdAt = query.createdAt
                this.updatedAt = query.updatedAt
            }
            .let { dsl.insertInto(RESUMES).set(it).returning().fetchOneInto(P_Resumes::class.java) }
            ?.toDomain()
            ?: throw IllegalStateException("Failed to insert resume and return the record")

    /**
     * Partial update for resume row.
     */
    override fun updateById(id: UUID, query: ResumeUpdateQuery): Int =
        dsl.newRecord(RESUMES)
            .apply {
                query.filename?.let { set(RESUMES.filename, it) }
                query.content?.let { set(RESUMES.content, it) }
                query.fileHash?.let { set(RESUMES.fileHash, it) }
                query.size?.let { set(RESUMES.size, it) }
                query.storageEngine?.let { set(RESUMES.storageEngine, it) }
                query.storageKey?.let { set(RESUMES.storageKey, it) }
                query.status?.let { set(RESUMES.status, it.toJooq()) }
                query.error?.let { set(RESUMES.error, it) }
                set(RESUMES.updatedAt, query.updatedAt)
            }
            .let { record ->
                if (record.modified()) {
                    dsl.update(RESUMES).set(record).where(RESUMES.id.eq(id)).execute()
                } else {
                    0
                }
            }
}

/**
 * Domain-to-jOOQ enum conversion.
 */
internal fun ResumeStatus.toJooq(): E_ResumeStatus = E_ResumeStatus.valueOf(name)

/**
 * jOOQ-to-domain enum conversion.
 */
internal fun E_ResumeStatus.toDomain(): ResumeStatus = ResumeStatus.fromString(name)

/**
 * jOOQ POJO to domain object mapping.
 */
internal fun P_Resumes.toDomain(): Resume =
    Resume(
        id = requireNotNull(id) { "Resume.id is null" },
        accountId = requireNotNull(accountId) { "Resume.accountId is null" },
        filename = requireNotNull(filename) { "Resume.filename is null" },
        content = content,
        fileHash = requireNotNull(fileHash) { "Resume.fileHash is null" },
        size = requireNotNull(size) { "Resume.size is null" },
        storageEngine = requireNotNull(storageEngine) { "Resume.storageEngine is null" },
        storageKey = requireNotNull(storageKey) { "Resume.storageKey is null" },
        status = requireNotNull(status) { "Resume.status is null" }.toDomain(),
        error = error,
        createdAt = requireNotNull(createdAt) { "Resume.createdAt is null" },
        updatedAt = requireNotNull(updatedAt) { "Resume.updatedAt is null" },
    )
