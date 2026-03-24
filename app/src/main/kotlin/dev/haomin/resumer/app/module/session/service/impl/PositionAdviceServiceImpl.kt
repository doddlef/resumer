package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.ai.LLMClient
import dev.haomin.resumer.app.common.ai.StructuredOutputInvoker
import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.session.domain.RewriteSuggestion
import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.SessionAdviceRepo
import dev.haomin.resumer.app.module.session.repo.query.SessionAdviceInsertQuery
import dev.haomin.resumer.app.module.session.service.PositionAdviceService
import dev.haomin.resumer.app.module.session.service.dto.PositionAdviceGenerateCmd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class PositionAdviceServiceImpl(
    private val applicationSessionRepo: ApplicationSessionRepo,
    private val sessionAdviceRepo: SessionAdviceRepo,
    private val llmClient: LLMClient,
    private val structuredOutputInvoker: StructuredOutputInvoker,
    private val objectMapper: ObjectMapper,
    @Value("classpath:prompt/position-advice-system.st") private val systemPromptResource: Resource,
    @Value("classpath:prompt/position-advice-user.st") private val userPromptResource: Resource,
) : PositionAdviceService {

    private companion object {
        private val logger = LoggerFactory.getLogger(PositionAdviceServiceImpl::class.java)
        private const val MAX_SUMMARY_LENGTH = 800
        private const val MAX_LIST_ITEMS = 10
        private const val MAX_ITEM_LENGTH = 200
    }

    override fun generate(cmd: PositionAdviceGenerateCmd): SessionAdvice {
        val session = applicationSessionRepo.selectById(cmd.sessionId)
            ?: throw NotFoundException("application session not found")
        if (session.accountId != cmd.accountId) {
            throw NotFoundException("application session not found")
        }

        logger.info(
            "Start position advice generation: sessionId={}, accountId={}, traceId={}",
            session.id,
            cmd.accountId,
            cmd.traceId,
        )

        return runCatching {
            val parsed = generateWithLlm(session.company, session.position, session.jobDescription, cmd.traceId)
            SessionAdviceInsertQuery(
                sessionId = session.id,
                positionSummary = parsed.summary.trim().take(MAX_SUMMARY_LENGTH).ifBlank { "No summary generated" },
                keyRequirements = normalizeList(parsed.keyRequirements),
                likelyInterviewFocus = normalizeList(parsed.likelyInterviewFocus),
                redFlags = normalizeList(parsed.redFlags),
                fitScore = 0,
                gaps = emptyList(),
                rewriteSuggestions = emptyList<RewriteSuggestion>(),
            ).let { sessionAdviceRepo.insertAndReturn(it) }
        }.onSuccess {
            logger.info("Position advice generated: sessionId={}, adviceId={}, traceId={}", session.id, it.id, cmd.traceId)
        }.onFailure { error ->
            logger.error("Position advice generation failed: sessionId={}, traceId={}, error={}", session.id, cmd.traceId, error.message, error)
        }.getOrElse {
            throw AppException(message = "failed to generate position advice", cause = it)
        }
    }

    private fun generateWithLlm(company: String, position: String, jobDescription: String, traceId: String): LlmPositionAdviceResult {
        val systemPrompt = readText(systemPromptResource)
        val userPrompt = readText(userPromptResource)
            .replace("\$company\$", company)
            .replace("\$position\$", position)
            .replace("\$jobDescription\$", jobDescription)

        return structuredOutputInvoker.invoke(
            systemPromptWithFormat = systemPrompt,
            userPrompt = userPrompt,
            logContext = "position_advice:$traceId",
            errorPrefix = "position advice parse failed: ",
            modelCall = { system, user -> llmClient.chat(system, user) },
            decoder = { raw -> objectMapper.readValue(raw, LlmPositionAdviceResult::class.java) },
        )
    }

    private fun normalizeList(values: List<String>): List<String> =
        values.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.take(MAX_ITEM_LENGTH) }
            .take(MAX_LIST_ITEMS)
            .toList()

    private fun readText(resource: Resource): String =
        resource.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

    private data class LlmPositionAdviceResult(
        val summary: String,
        val keyRequirements: List<String>,
        val likelyInterviewFocus: List<String>,
        val redFlags: List<String>,
    )
}
