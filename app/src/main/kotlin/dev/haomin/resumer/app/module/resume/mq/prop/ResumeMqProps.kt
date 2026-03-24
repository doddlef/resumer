package dev.haomin.resumer.app.module.resume.mq.prop

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resumer.resume.mq")
data class ResumeMqProps(
    val enabled: Boolean = false,
    val streamKey: String = "resumer.resume.analysis.v1",
    val dlqStreamKey: String = "resumer.resume.analysis.v1.dlq",
    val streamMaxLen: Long = 100_000,
    val dlqStreamMaxLen: Long = 10_000,
    val groupName: String = "resume-analysis-workers",
    val consumerPrefix: String = "resume-analysis",
    val batchSize: Int = 10,
    val pollBlockMs: Long = 5000,
    val reclaimIdleMs: Long = 60_000,
    val reclaimBatchSize: Int = 10,
    val maxRetry: Int = 3,
)
