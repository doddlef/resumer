package dev.haomin.resumer.app.common.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resumer.ai")
data class AiProperties(
    val provider: AiProvider = AiProvider.OLLAMA,
    val model: String = "llama3:8b",
    val temperature: Double = 0.2,
)

enum class AiProvider {
    OLLAMA,
    OPENAI,
}
