package dev.haomin.resumer.app.module.session.mq

import dev.haomin.resumer.app.framework.redis.RedisClient
import dev.haomin.resumer.app.infra.mq.AbstractStreamPublisher
import dev.haomin.resumer.app.infra.mq.StreamSerde
import dev.haomin.resumer.app.infra.mq.model.MessageStatus
import dev.haomin.resumer.app.infra.mq.model.MessageWrapper
import dev.haomin.resumer.app.module.session.mq.model.AdviceJobType
import dev.haomin.resumer.app.module.session.mq.model.SessionAdviceJobPayload
import dev.haomin.resumer.app.module.session.mq.prop.SessionMqProps
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SessionAdvicePublisher(
    redisClient: RedisClient,
    private val props: SessionMqProps,
    override val serde: StreamSerde<SessionAdviceJobPayload>,
) : AbstractStreamPublisher<SessionAdviceJobPayload>(redisClient) {

    override val streamKey: String
        get() = props.streamKey

    override val streamMaxLen: Long?
        get() = props.streamMaxLen

    fun publish(sessionId: UUID, accountId: UUID, jobType: AdviceJobType, traceId: String): String {
        val now = System.currentTimeMillis()
        val wrapper = MessageWrapper(
            id = sessionId.toString(),
            type = MESSAGE_TYPE,
            payload = SessionAdviceJobPayload(
                sessionId = sessionId,
                accountId = accountId,
                jobType = jobType,
            ),
            attempt = 0,
            status = MessageStatus.PENDING,
            traceId = traceId,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
        return publish(wrapper)
    }

    private companion object {
        const val MESSAGE_TYPE = "session_advice_job"
    }
}
