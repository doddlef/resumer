package dev.haomin.resumer.app.module.resume.mq

import dev.haomin.resumer.app.framework.redis.RedisClient
import dev.haomin.resumer.app.infra.mq.AbstractStreamPublisher
import dev.haomin.resumer.app.infra.mq.StreamSerde
import dev.haomin.resumer.app.infra.mq.model.MessageStatus
import dev.haomin.resumer.app.infra.mq.model.MessageWrapper
import dev.haomin.resumer.app.module.resume.mq.model.ResumeAnalyzePayload
import dev.haomin.resumer.app.module.resume.mq.prop.ResumeMqProps
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ResumeAnalyzePublisher(
    redisClient: RedisClient,
    private val props: ResumeMqProps,
    override val serde: StreamSerde<ResumeAnalyzePayload>,
) : AbstractStreamPublisher<ResumeAnalyzePayload>(redisClient) {

    override val streamKey: String
        get() = props.streamKey

    override val streamMaxLen: Long?
        get() = props.streamMaxLen

    fun publishResumeAnalyze(resumeId: UUID, accountId: UUID, traceId: String): String {
        val now = System.currentTimeMillis()
        val wrapper = MessageWrapper(
            id = resumeId.toString(),
            type = MESSAGE_TYPE,
            payload = ResumeAnalyzePayload(
                resumeId = resumeId,
                accountId = accountId,
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
        const val MESSAGE_TYPE = "resume_analysis"
    }
}
