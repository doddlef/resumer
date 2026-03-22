package dev.haomin.resumer.app.auth.repo.query

import java.time.OffsetDateTime

data class RefreshSessionUpdateQuery(
    val secret: String? = null,
    val prevSecret: String? = null,
    val lastUsedAt: OffsetDateTime? = null,
    val revokedAt: OffsetDateTime? = null,
    val revokeReason: String? = null,
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
