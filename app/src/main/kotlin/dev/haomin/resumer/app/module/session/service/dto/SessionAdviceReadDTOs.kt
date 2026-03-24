package dev.haomin.resumer.app.module.session.service.dto

import java.util.UUID

data class SessionAdviceReadCmd(
    val sessionId: UUID,
    val accountId: UUID,
)
