package dev.haomin.resumer.app.common.ai

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfiguration {
    @Bean
    fun llmClient(
        chatClientBuilder: ChatClient.Builder,
    ): LLMClient = SpringAiLLMClient(chatClientBuilder)
}
