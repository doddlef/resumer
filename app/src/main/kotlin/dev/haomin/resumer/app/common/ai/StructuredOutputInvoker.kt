package dev.haomin.resumer.app.common.ai

import dev.haomin.resumer.app.common.exception.AppException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Shared helper for LLM structured output calls with retry.
 *
 * This class is model-client agnostic. Callers inject:
 * 1) [modelCall]: how to call the model and get raw text
 * 2) [decoder]: how to decode raw text to target object
 */
@Component
class StructuredOutputInvoker(
    private val props: StructuredOutputProperties,
) {

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(StructuredOutputInvoker::class.java)

        const val STRICT_JSON_INSTRUCTION = """
Return ONLY one valid JSON object that can be parsed directly.
Rules:
1) Do NOT use markdown code fences like ```json.
2) Do NOT add explanations, comments, or prefixes/suffixes.
3) Ensure all quotes are properly escaped.
"""
    }

    fun <T> invoke(
        systemPromptWithFormat: String,
        userPrompt: String,
        logContext: String,
        errorPrefix: String = "failed to generate structured output: ",
        modelCall: (systemPrompt: String, userPrompt: String) -> String,
        decoder: (rawOutput: String) -> T,
    ): T {
        val attempts = props.maxAttempts.coerceAtLeast(1)
        var lastError: Exception? = null

        for (attempt in 1..attempts) {
            val attemptSystemPrompt = if (attempt == 1) {
                systemPromptWithFormat
            } else {
                buildRetrySystemPrompt(systemPromptWithFormat, lastError)
            }

            try {
                val rawOutput = modelCall(attemptSystemPrompt, userPrompt)
                return decoder(rawOutput)
            } catch (e: Exception) {
                lastError = e
                logger.warn(
                    "{} structured output failed, will retry: attempt={}, maxAttempts={}, error={}",
                    logContext,
                    attempt,
                    attempts,
                    e.message,
                )
            }
        }

        val detail = sanitizeErrorMessage(lastError?.message)
        throw AppException(message = "$errorPrefix$detail", cause = lastError)
    }

    private fun buildRetrySystemPrompt(systemPromptWithFormat: String, lastError: Exception?): String {
        return buildString {
            append(systemPromptWithFormat)
            append("\n\n")
            append(STRICT_JSON_INSTRUCTION)
            append("\nPrevious output could not be parsed. Return valid JSON only.")

            if (props.includeLastErrorInRetryPrompt && lastError?.message != null) {
                append("\nPrevious parse failure: ")
                append(sanitizeErrorMessage(lastError.message))
            }
        }
    }

    private fun sanitizeErrorMessage(message: String?): String {
        val oneLine = message
            ?.replace('\n', ' ')
            ?.replace('\r', ' ')
            ?.trim()
            .orEmpty()

        if (oneLine.isBlank()) {
            return "unknown"
        }

        val maxLength = props.maxErrorMessageLength.coerceAtLeast(32)
        return if (oneLine.length > maxLength) {
            "${oneLine.take(maxLength)}..."
        } else {
            oneLine
        }
    }
}
