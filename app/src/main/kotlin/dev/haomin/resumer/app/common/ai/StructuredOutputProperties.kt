package dev.haomin.resumer.app.common.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resumer.ai.structured")
data class StructuredOutputProperties(
    val maxAttempts: Int = 2,
    val includeLastErrorInRetryPrompt: Boolean = true,
    val maxErrorMessageLength: Int = 200,
)
