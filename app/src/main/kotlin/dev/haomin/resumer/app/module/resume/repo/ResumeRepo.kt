package dev.haomin.resumer.app.module.resume.repo

import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.repo.query.ResumeInsertQuery
import dev.haomin.resumer.app.module.resume.repo.query.ResumeUpdateQuery
import java.util.UUID

/**
 * Repository contract for resume persistence.
 */
interface ResumeRepo {

    /**
     * Reads a resume by primary key.
     */
    fun selectById(id: UUID): Resume?

    /**
     * Reads a resume by account id and file hash.
     */
    fun selectByAccountIdAndFileHash(accountId: UUID, fileHash: String): Resume?

    /**
     * Inserts a resume and returns the inserted row as a domain object.
     */
    fun insertAndReturn(query: ResumeInsertQuery): Resume

    /**
     * Updates a resume by id.
     *
     * @return number of rows affected.
     */
    fun updateById(id: UUID, query: ResumeUpdateQuery): Int
}
