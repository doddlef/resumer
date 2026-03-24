package dev.haomin.resumer.app.common.ai

import dev.haomin.resumer.app.common.exception.AppException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient

class SpringAiLLMClient(
    chatClientBuilder: ChatClient.Builder,
    private val props: AiProperties,
) : LLMClient {

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SpringAiLLMClient::class.java)
    }

    private val chatClient: ChatClient = chatClientBuilder.build()

    override fun chat(systemPrompt: String, userPrompt: String): String {
        val response = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content()
            ?.trim()
            .orEmpty()

        if (response.isBlank()) {
            throw AppException(message = "empty response from ai model")
        }

        logger.debug(
            "AI model response received: provider={}, model={}, chars={}",
            props.provider,
            props.model,
            response.length,
        )
        return response
    }
}
