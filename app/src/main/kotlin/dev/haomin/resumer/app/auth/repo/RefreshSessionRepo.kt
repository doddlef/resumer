package dev.haomin.resumer.app.auth.repo

import dev.haomin.resumer.app.auth.domain.RefreshSession
import dev.haomin.resumer.app.auth.repo.query.RefreshSessionInsertQuery
import dev.haomin.resumer.app.auth.repo.query.RefreshSessionUpdateQuery
import java.util.UUID

/**
 * Repository contract for refresh session persistence.
 */
interface RefreshSessionRepo {

    /**
     * Inserts a refresh session.
     *
     * @return number of rows affected.
     */
    fun insert(query: RefreshSessionInsertQuery): Int

    /**
     * Reads a refresh session by id with row lock (`FOR UPDATE`).
     */
    fun selectByIdForUpdate(id: UUID): RefreshSession?

    /**
     * Updates a refresh session by id.
     *
     * @return number of rows affected.
     */
    fun updateById(id: UUID, query: RefreshSessionUpdateQuery): Int
}
