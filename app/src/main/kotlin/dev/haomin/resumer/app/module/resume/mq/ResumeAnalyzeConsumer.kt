package dev.haomin.resumer.app.module.resume.mq

import dev.haomin.resumer.app.framework.redis.RedisClient
import dev.haomin.resumer.app.infra.mq.AbstractStreamConsumer
import dev.haomin.resumer.app.infra.mq.FixedRetryPolicy
import dev.haomin.resumer.app.infra.mq.RetryPolicy
import dev.haomin.resumer.app.infra.mq.StreamSerde
import dev.haomin.resumer.app.module.resume.mq.model.ResumeAnalyzePayload
import dev.haomin.resumer.app.module.resume.mq.prop.ResumeMqProps
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "resumer.resume.mq", name = ["enabled"], havingValue = "true")
class ResumeAnalyzeConsumer(
    redisClient: RedisClient,
    private val props: ResumeMqProps,
    override val serde: StreamSerde<ResumeAnalyzePayload>,
    private val processor: ResumeAnalysisJobProcessor,
) : AbstractStreamConsumer<ResumeAnalyzePayload>(redisClient) {

    override val streamKey: String
        get() = props.streamKey

    override val streamMaxLen: Long?
        get() = props.streamMaxLen

    override val dlqStreamKey: String
        get() = props.dlqStreamKey

    override val dlqStreamMaxLen: Long?
        get() = props.dlqStreamMaxLen

    override val groupName: String
        get() = props.groupName

    override val consumerPrefix: String
        get() = props.consumerPrefix

    override val batchSize: Int
        get() = props.batchSize

    override val pollBlockMs: Long
        get() = props.pollBlockMs

    override val reclaimIdleMs: Long
        get() = props.reclaimIdleMs

    override val reclaimBatchSize: Int
        get() = props.reclaimBatchSize

    override val retryPolicy: RetryPolicy
        get() = FixedRetryPolicy(props.maxRetry)

    override fun handleBusiness(payload: ResumeAnalyzePayload) {
        // Placeholder: this delegates to a temporary processor implementation.
        processor.process(payload)
    }
}
