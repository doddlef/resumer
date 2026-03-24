package dev.haomin.resumer.app.module.session.repo

import dev.haomin.resumer.app.module.session.domain.ApplicationSession
import dev.haomin.resumer.app.module.session.repo.query.ApplicationSessionInsertQuery
import dev.haomin.resumer.app.module.session.repo.query.ApplicationSessionUpdateQuery
import java.util.UUID

interface ApplicationSessionRepo {
    fun selectById(id: UUID): ApplicationSession?

    fun selectByAccountId(accountId: UUID): List<ApplicationSession>

    fun insertAndReturn(query: ApplicationSessionInsertQuery): ApplicationSession

    fun updateById(id: UUID, query: ApplicationSessionUpdateQuery): Int
}
