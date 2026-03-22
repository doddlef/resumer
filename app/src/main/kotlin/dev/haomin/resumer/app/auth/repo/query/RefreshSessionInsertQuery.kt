package dev.haomin.resumer.app.auth.repo.query

import dev.haomin.resumer.app.common.id.UUIDGenerator
import java.time.OffsetDateTime
import java.util.UUID

data class RefreshSessionInsertQuery(
    val id: UUID = UUIDGenerator.next(),
    val accountId: UUID,
    val secret: String,
    val prevSecret: String? = null,
    val lastUsedAt: OffsetDateTime? = null,
    val revokedAt: OffsetDateTime? = null,
    val revokeReason: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
