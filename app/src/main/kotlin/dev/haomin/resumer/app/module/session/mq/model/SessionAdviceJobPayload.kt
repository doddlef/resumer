package dev.haomin.resumer.app.module.session.mq.model

import java.util.UUID

data class SessionAdviceJobPayload(
    val sessionId: UUID,
    val accountId: UUID,
    val jobType: AdviceJobType,
)
