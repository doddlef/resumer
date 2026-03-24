package dev.haomin.resumer.app.module.session.service.dto

import dev.haomin.resumer.app.module.session.domain.TaskStatus
import dev.haomin.resumer.app.module.session.mq.model.AdviceJobType
import java.util.UUID

data class QueueSessionAdviceTaskCmd(
    val sessionId: UUID,
    val accountId: UUID,
    val jobType: AdviceJobType,
)

data class QueueSessionAdviceTaskResult(
    val sessionId: UUID,
    val jobType: AdviceJobType,
    val status: TaskStatus,
    val traceId: String,
)
