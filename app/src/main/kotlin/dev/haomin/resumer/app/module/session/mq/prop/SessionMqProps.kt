package dev.haomin.resumer.app.module.session.mq.prop

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resumer.session.mq")
data class SessionMqProps(
    val streamKey: String = "resumer.session.advice.v1",
    val dlqStreamKey: String = "resumer.session.advice.v1.dlq",
    val streamMaxLen: Long = 100_000,
    val dlqStreamMaxLen: Long = 10_000,
    val groupName: String = "session-advice-workers",
    val consumerPrefix: String = "session-advice",
    val batchSize: Int = 10,
    val pollBlockMs: Long = 5000,
    val reclaimIdleMs: Long = 60_000,
    val reclaimBatchSize: Int = 10,
    val maxRetry: Int = 3,
)
