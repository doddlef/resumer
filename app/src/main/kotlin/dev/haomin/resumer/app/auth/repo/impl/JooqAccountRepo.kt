package dev.haomin.resumer.app.auth.repo.impl

import dev.haomin.filesheep.jooq.enums.E_AccountRole
import dev.haomin.filesheep.jooq.enums.E_AccountStatus
import dev.haomin.filesheep.jooq.tables.pojos.P_Accounts
import dev.haomin.filesheep.jooq.tables.references.ACCOUNTS
import dev.haomin.resumer.app.auth.domain.Account
import dev.haomin.resumer.app.auth.domain.AccountRole
import dev.haomin.resumer.app.auth.domain.AccountStatus
import dev.haomin.resumer.app.auth.repo.AccountRepo
import dev.haomin.resumer.app.auth.repo.query.AccountInsertQuery
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * jOOQ-based implementation of [AccountRepo].
 */
@Repository
class JooqAccountRepo(
    private val dsl: DSLContext,
) : AccountRepo {

    /**
     * Fetch account by id.
     */
    override fun selectById(id: UUID): Account? =
        dsl.selectFrom(ACCOUNTS)
            .where(ACCOUNTS.id.eq(id))
            .fetchOneInto(P_Accounts::class.java)
            ?.toDomain()

    /**
     * Fetch account by email.
     */
    override fun selectByEmail(email: String): Account? =
        dsl.selectFrom(ACCOUNTS)
            .where(ACCOUNTS.email.eq(email))
            .fetchOneInto(P_Accounts::class.java)
            ?.toDomain()

    /**
     * Insert account and return inserted row.
     */
    override fun insertAndReturn(query: AccountInsertQuery): Account =
        dsl.newRecord(ACCOUNTS)
            .apply {
                this.id = query.id
                this.email = query.email
                this.password = query.password
                this.name = query.name
                this.status = query.status.toJooq()
                this.role = query.role.toJooq()
                this.createdAt = query.createdAt
                this.updatedAt = query.updatedAt
            }
            .let { dsl.insertInto(ACCOUNTS).set(it).returning().fetchOneInto(P_Accounts::class.java) }
            ?.toDomain()
            ?: throw IllegalStateException("Failed to insert account and return the record")
}

/**
 * Domain-to-jOOQ enum conversion.
 */
internal fun AccountStatus.toJooq(): E_AccountStatus = E_AccountStatus.valueOf(name)

/**
 * jOOQ-to-domain enum conversion.
 */
internal fun E_AccountStatus.toDomain(): AccountStatus = AccountStatus.fromString(name)

/**
 * Domain-to-jOOQ enum conversion.
 */
internal fun AccountRole.toJooq(): E_AccountRole = E_AccountRole.valueOf(name)

/**
 * jOOQ-to-domain enum conversion.
 */
internal fun E_AccountRole.toDomain(): AccountRole = AccountRole.fromString(name)

/**
 * jOOQ POJO to domain object mapping.
 */
internal fun P_Accounts.toDomain(): Account =
    Account(
        id = requireNotNull(id) { "Account.id is null" },
        email = requireNotNull(email) { "Account.email is null" },
        password = requireNotNull(password) { "Account.password is null" },
        name = requireNotNull(name) { "Account.name is null" },
        status = requireNotNull(status) { "Account.status is null" }.toDomain(),
        role = requireNotNull(role) { "Account.role is null" }.toDomain(),
        createdAt = requireNotNull(createdAt) { "Account.createdAt is null" },
        updatedAt = requireNotNull(updatedAt) { "Account.updatedAt is null" },
    )
