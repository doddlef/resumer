package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.common.ai.LLMClient
import dev.haomin.resumer.app.common.ai.StructuredOutputInvoker
import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.resume.domain.ResumeAnalysis
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.domain.ResumeSuggestion
import dev.haomin.resumer.app.module.resume.repo.ResumeAnalysisRepo
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.resume.repo.query.ResumeAnalysisInsertQuery
import dev.haomin.resumer.app.module.resume.repo.query.ResumeUpdateQuery
import dev.haomin.resumer.app.module.resume.service.ResumeAnalysisService
import dev.haomin.resumer.app.module.resume.service.dto.ResumeAnalyzeCmd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class ResumeAnalysisServiceImpl(
    private val resumeRepo: ResumeRepo,
    private val resumeAnalysisRepo: ResumeAnalysisRepo,
    private val llmClient: LLMClient,
    private val structuredOutputInvoker: StructuredOutputInvoker,
    private val objectMapper: ObjectMapper,
    @Value("classpath:prompt/resume-analysis-system.st") private val systemPromptResource: Resource,
    @Value("classpath:prompt/resume-analysis-user.st") private val userPromptResource: Resource,
) : ResumeAnalysisService {

    private companion object {
        private val logger = LoggerFactory.getLogger(ResumeAnalysisServiceImpl::class.java)

        private const val MAX_SUMMARY_LENGTH = 500
        private const val MAX_STRENGTHS = 5
        private const val MAX_SUGGESTIONS = 8
        private const val MAX_ERROR_LENGTH = 500

        private val ALLOWED_CATEGORIES = setOf("content", "format", "skills", "project")
        private val ALLOWED_PRIORITIES = setOf("high", "medium", "low")
    }

    override fun analyze(cmd: ResumeAnalyzeCmd): ResumeAnalysis {
        val resume = resumeRepo.selectById(cmd.resumeId)
            ?: throw NotFoundException("resume not found: ${cmd.resumeId}")
        if (resume.accountId != cmd.accountId) {
            throw InvalidParamException("resume account mismatch")
        }
        if (resume.status == ResumeStatus.COMPLETED) {
            return resumeAnalysisRepo.selectLatestByResumeId(resume.id)
                ?: throw AppException(message = "resume marked completed but analysis missing")
        }
        val content = resume.content?.trim().orEmpty()
        if (content.isBlank()) {
            markFailed(resume.id, "resume content is empty")
            throw InvalidParamException("resume content is empty")
        }
        markAnalyzing(resume.id)
        logger.info("Start resume analysis: resumeId={}, accountId={}, traceId={}", resume.id, cmd.accountId, cmd.traceId)

        return runCatching {
            val parsed = analyzeWithLlm(content, cmd.traceId)
            ResumeAnalysisInsertQuery(
                resumeId = resume.id,
                score = parsed.score.coerceIn(0, 100),
                summary = parsed.summary.take(MAX_SUMMARY_LENGTH).ifBlank { "No summary generated" },
                strengths = parsed.strengths
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .take(MAX_STRENGTHS),
                suggestions = parsed.suggestions
                    .map { it.normalize() }
                    .filter { it.issue.isNotBlank() && it.recommendation.isNotBlank() }
                    .take(MAX_SUGGESTIONS),
            ).let { resumeAnalysisRepo.insertAndReturn(it) }
        }.onSuccess {
            resumeRepo.updateById(resume.id, ResumeUpdateQuery(status = ResumeStatus.COMPLETED))
            logger.info("Resume analysis completed: resumeId={}, analysisId={}, traceId={}", resume.id, it.id, cmd.traceId)
        }.onFailure { error ->
            val message = sanitizeError(error.message)
            markFailed(resume.id, message)
            logger.error("Resume analysis failed: resumeId={}, traceId={}, error={}", resume.id, cmd.traceId, message, error)
        }.getOrElse {
            throw AppException(message = "resume analysis failed", cause = it)
        }
    }

    private fun analyzeWithLlm(content: String, traceId: String): LlmResumeAnalysisResult {
        val systemPrompt = readText(systemPromptResource)
        val userPrompt = readText(userPromptResource)
            .replace("\$resumeText\$", content)

        return structuredOutputInvoker.invoke(
            systemPromptWithFormat = systemPrompt,
            userPrompt = userPrompt,
            logContext = "resume_analysis:$traceId",
            errorPrefix = "resume analysis parse failed: ",
            modelCall = { system, user -> llmClient.chat(system, user) },
            decoder = { raw -> objectMapper.readValue(raw, LlmResumeAnalysisResult::class.java) },
        )
    }

    private fun markAnalyzing(resumeId: UUID) {
        resumeRepo.updateById(
            resumeId,
            ResumeUpdateQuery(status = ResumeStatus.ANALYZING),
        )
    }

    private fun markFailed(resumeId: UUID, reason: String) {
        resumeRepo.updateById(
            resumeId,
            ResumeUpdateQuery(
                status = ResumeStatus.FAILED,
                error = reason.take(MAX_ERROR_LENGTH),
            ),
        )
    }

    private fun readText(resource: Resource): String =
        resource.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun sanitizeError(message: String?): String =
        message
            ?.replace('\n', ' ')
            ?.replace('\r', ' ')
            ?.trim()
            ?.take(MAX_ERROR_LENGTH)
            .orEmpty()
            .ifBlank { "unknown error" }

    private fun LlmSuggestion.normalize(): ResumeSuggestion =
        ResumeSuggestion(
            category = category.lowercase().takeIf { it in ALLOWED_CATEGORIES } ?: "content",
            priority = priority.lowercase().takeIf { it in ALLOWED_PRIORITIES } ?: "medium",
            issue = issue.trim(),
            recommendation = recommendation.trim(),
        )

    private data class LlmResumeAnalysisResult(
        val score: Int,
        val summary: String,
        val strengths: List<String>,
        val suggestions: List<LlmSuggestion>,
    )

    private data class LlmSuggestion(
        val category: String,
        val priority: String,
        val issue: String,
        val recommendation: String,
    )
}
