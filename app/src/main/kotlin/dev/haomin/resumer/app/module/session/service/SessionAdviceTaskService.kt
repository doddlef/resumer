package dev.haomin.resumer.app.module.session.service

import dev.haomin.resumer.app.module.session.mq.model.AdviceJobType
import dev.haomin.resumer.app.module.session.service.dto.QueueSessionAdviceTaskCmd
import dev.haomin.resumer.app.module.session.service.dto.QueueSessionAdviceTaskResult

interface SessionAdviceTaskService {
    fun queue(cmd: QueueSessionAdviceTaskCmd): QueueSessionAdviceTaskResult

    fun queuePositionAdvice(sessionId: java.util.UUID, accountId: java.util.UUID): QueueSessionAdviceTaskResult =
        queue(
            QueueSessionAdviceTaskCmd(
                sessionId = sessionId,
                accountId = accountId,
                jobType = AdviceJobType.POSITION_ADVICE,
            ),
        )

    fun queueResumeFit(sessionId: java.util.UUID, accountId: java.util.UUID): QueueSessionAdviceTaskResult =
        queue(
            QueueSessionAdviceTaskCmd(
                sessionId = sessionId,
                accountId = accountId,
                jobType = AdviceJobType.RESUME_FIT,
            ),
        )
}
