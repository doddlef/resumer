package dev.haomin.resumer.app.auth.repo.query

import dev.haomin.resumer.app.auth.domain.AccountRole
import dev.haomin.resumer.app.auth.domain.AccountStatus
import dev.haomin.resumer.app.common.id.UUIDGenerator
import java.time.OffsetDateTime
import java.util.UUID

data class AccountInsertQuery(
    val id: UUID = UUIDGenerator.next(),
    val email: String,
    val password: String,
    val name: String,
    val status: AccountStatus = AccountStatus.ACTIVE,
    val role: AccountRole = AccountRole.USER,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
