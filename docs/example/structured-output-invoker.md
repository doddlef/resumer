# StructuredOutputInvoker (Reference)

## Goal
Provide one reusable entrypoint for LLM structured output with retry and stricter JSON instructions.

## Why
- Avoid repeating retry logic in each service.
- Keep model provider integration flexible (`Ollama`, OpenAI-compatible, etc.).
- Make parse failures observable and predictable.

## Kotlin Example
```kotlin
@Component
class StructuredOutputInvoker(
    private val props: StructuredOutputProperties,
) {

    fun <T> invoke(
        systemPromptWithFormat: String,
        userPrompt: String,
        logContext: String,
        errorPrefix: String = "failed to generate structured output: ",
        modelCall: (systemPrompt: String, userPrompt: String) -> String,
        decoder: (rawOutput: String) -> T,
    ): T {
        // 1) build attempt prompt
        // 2) call model
        // 3) decode output
        // 4) on failure, retry with strict JSON instruction
        // 5) if all attempts fail, throw AppException
    }
}
```

## Suggested Properties
```yaml
resumer:
  ai:
    structured:
      max-attempts: 2
      include-last-error-in-retry-prompt: true
      max-error-message-length: 200
```

## Usage Example
```kotlin
val result = structuredOutputInvoker.invoke(
    systemPromptWithFormat = promptTemplate,
    userPrompt = resumeText,
    logContext = "resume_analysis",
    modelCall = { system, user -> llmClient.complete(system, user) },
    decoder = { raw -> objectMapper.readValue(raw, ResumeAnalysisLLMResult::class.java) },
)
```

## Notes
- Keep deterministic score calculation outside of LLM when possible.
- Do not parse markdown code blocks as JSON; force model to return pure JSON.
- Keep `decoder` close to domain model (e.g., analysis DTO in module layer).
