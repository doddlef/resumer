package dev.haomin.resumer.app.common.ai

/**
 * Unified abstraction for chat completion providers.
 */
interface LLMClient {
    fun chat(systemPrompt: String, userPrompt: String): String
}
