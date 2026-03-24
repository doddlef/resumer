package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.ai.LLMClient
import dev.haomin.resumer.app.common.ai.StructuredOutputInvoker
import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.session.domain.RewriteSuggestion
import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.SessionAdviceRepo
import dev.haomin.resumer.app.module.session.repo.query.SessionAdviceInsertQuery
import dev.haomin.resumer.app.module.session.service.ResumeFitAdviceService
import dev.haomin.resumer.app.module.session.service.dto.ResumeFitAdviceGenerateCmd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class ResumeFitAdviceServiceImpl(
    private val applicationSessionRepo: ApplicationSessionRepo,
    private val resumeRepo: ResumeRepo,
    private val sessionAdviceRepo: SessionAdviceRepo,
    private val llmClient: LLMClient,
    private val structuredOutputInvoker: StructuredOutputInvoker,
    private val objectMapper: ObjectMapper,
    @Value("classpath:prompt/resume-fit-system.st") private val systemPromptResource: Resource,
    @Value("classpath:prompt/resume-fit-user.st") private val userPromptResource: Resource,
) : ResumeFitAdviceService {

    private companion object {
        private val logger = LoggerFactory.getLogger(ResumeFitAdviceServiceImpl::class.java)
        private const val MAX_GAPS = 10
        private const val MAX_ITEM_LENGTH = 220
        private const val MAX_REWRITE_SUGGESTIONS = 10
        private val ALLOWED_PRIORITIES = setOf("high", "medium", "low")
    }

    override fun generate(cmd: ResumeFitAdviceGenerateCmd): SessionAdvice {
        val session = applicationSessionRepo.selectById(cmd.sessionId)
            ?: throw NotFoundException("application session not found")
        if (session.accountId != cmd.accountId) {
            throw NotFoundException("application session not found")
        }
        val resumeId = session.resumeId ?: throw InvalidParamException("resume is required for resume-fit analysis")
        val resume = resumeRepo.selectById(resumeId) ?: throw NotFoundException("resume not found")
        if (resume.accountId != cmd.accountId) {
            throw NotFoundException("resume not found")
        }
        val resumeText = resume.content?.trim().orEmpty()
        if (resumeText.isBlank()) {
            throw InvalidParamException("resume content is empty")
        }

        val latest = sessionAdviceRepo.selectLatestBySessionId(session.id)
        logger.info(
            "Start resume fit advice generation: sessionId={}, accountId={}, traceId={}",
            session.id,
            cmd.accountId,
            cmd.traceId,
        )

        return runCatching {
            val parsed = generateWithLlm(
                company = session.company,
                position = session.position,
                jobDescription = session.jobDescription,
                resumeText = resumeText,
                keyRequirements = latest?.keyRequirements.orEmpty(),
                traceId = cmd.traceId,
            )
            SessionAdviceInsertQuery(
                sessionId = session.id,
                positionSummary = latest?.positionSummary ?: "No position advice generated",
                keyRequirements = latest?.keyRequirements.orEmpty(),
                likelyInterviewFocus = latest?.likelyInterviewFocus.orEmpty(),
                redFlags = latest?.redFlags.orEmpty(),
                fitScore = parsed.fitScore.coerceIn(0, 100),
                gaps = normalizeTextList(parsed.gaps),
                rewriteSuggestions = parsed.rewriteSuggestions
                    .map { it.normalize() }
                    .filter { it.issue.isNotBlank() && it.recommendation.isNotBlank() }
                    .take(MAX_REWRITE_SUGGESTIONS),
            ).let { sessionAdviceRepo.insertAndReturn(it) }
        }.onSuccess {
            logger.info("Resume fit advice generated: sessionId={}, adviceId={}, traceId={}", session.id, it.id, cmd.traceId)
        }.onFailure { error ->
            logger.error("Resume fit advice generation failed: sessionId={}, traceId={}, error={}", session.id, cmd.traceId, error.message, error)
        }.getOrElse {
            throw AppException(message = "failed to generate resume fit advice", cause = it)
        }
    }

    private fun generateWithLlm(
        company: String,
        position: String,
        jobDescription: String,
        resumeText: String,
        keyRequirements: List<String>,
        traceId: String,
    ): LlmResumeFitResult {
        val systemPrompt = readText(systemPromptResource)
        val userPrompt = readText(userPromptResource)
            .replace($$"$company$", company)
            .replace($$"$position$", position)
            .replace($$"$jobDescription$", jobDescription)
            .replace(
                $$"$keyRequirements$",
                keyRequirements.joinToString(separator = "\n") { "- $it" }.ifBlank { "- not available" },
            )
            .replace($$"$resumeText$", resumeText)

        return structuredOutputInvoker.invoke(
            systemPromptWithFormat = systemPrompt,
            userPrompt = userPrompt,
            logContext = "resume_fit:$traceId",
            errorPrefix = "resume fit parse failed: ",
            modelCall = { system, user -> llmClient.chat(system, user) },
            decoder = { raw -> objectMapper.readValue(raw, LlmResumeFitResult::class.java) },
        )
    }

    private fun normalizeTextList(values: List<String>): List<String> =
        values.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.take(MAX_ITEM_LENGTH) }
            .take(MAX_GAPS)
            .toList()

    private fun readText(resource: Resource): String =
        resource.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun LlmRewriteSuggestion.normalize(): RewriteSuggestion =
        RewriteSuggestion(
            section = section.trim().ifBlank { "general" }.take(50),
            priority = priority.lowercase().takeIf { it in ALLOWED_PRIORITIES } ?: "medium",
            issue = issue.trim().take(MAX_ITEM_LENGTH),
            recommendation = recommendation.trim().take(MAX_ITEM_LENGTH),
            example = example?.trim()?.take(MAX_ITEM_LENGTH)?.ifBlank { null },
        )

    private data class LlmResumeFitResult(
        val fitScore: Int,
        val gaps: List<String>,
        val rewriteSuggestions: List<LlmRewriteSuggestion>,
    )

    private data class LlmRewriteSuggestion(
        val section: String,
        val priority: String,
        val issue: String,
        val recommendation: String,
        val example: String? = null,
    )
}
