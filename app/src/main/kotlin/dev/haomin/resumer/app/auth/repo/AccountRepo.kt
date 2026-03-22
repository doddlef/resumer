package dev.haomin.resumer.app.auth.repo

import dev.haomin.resumer.app.auth.domain.Account
import dev.haomin.resumer.app.auth.repo.query.AccountInsertQuery
import java.util.UUID

/**
 * Repository contract for account persistence.
 */
interface AccountRepo {

    /**
     * Reads account by primary key.
     */
    fun selectById(id: UUID): Account?

    /**
     * Reads account by email.
     */
    fun selectByEmail(email: String): Account?

    /**
     * Inserts an account and returns the inserted row as a domain object.
     */
    fun insertAndReturn(query: AccountInsertQuery): Account
}
