package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.ai.LLMClient
import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.session.domain.SessionCoverLetter
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.SessionAdviceRepo
import dev.haomin.resumer.app.module.session.repo.SessionCoverLetterRepo
import dev.haomin.resumer.app.module.session.repo.query.SessionCoverLetterInsertQuery
import dev.haomin.resumer.app.module.session.service.SessionCoverLetterService
import dev.haomin.resumer.app.module.session.service.dto.SessionCoverLetterGenerateCmd
import dev.haomin.resumer.app.module.session.service.dto.SessionCoverLetterGetCmd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
class SessionCoverLetterServiceImpl(
    private val applicationSessionRepo: ApplicationSessionRepo,
    private val resumeRepo: ResumeRepo,
    private val sessionAdviceRepo: SessionAdviceRepo,
    private val sessionCoverLetterRepo: SessionCoverLetterRepo,
    private val llmClient: LLMClient,
    @Value("classpath:prompt/cover-letter-system.st") private val systemPromptResource: Resource,
    @Value("classpath:prompt/cover-letter-user.st") private val userPromptResource: Resource,
) : SessionCoverLetterService {

    private companion object {
        private val logger = LoggerFactory.getLogger(SessionCoverLetterServiceImpl::class.java)
        private const val MAX_CONTENT_LENGTH = 12_000
    }

    override fun generate(cmd: SessionCoverLetterGenerateCmd): SessionCoverLetter {
        val session = applicationSessionRepo.selectById(cmd.sessionId)
            ?: throw NotFoundException("application session not found")
        if (session.accountId != cmd.accountId) {
            throw NotFoundException("application session not found")
        }
        val resumeText = session.resumeId
            ?.let { resumeRepo.selectById(it) }
            ?.takeIf { it.accountId == cmd.accountId }
            ?.content
            .orEmpty()
        val advice = sessionAdviceRepo.selectLatestBySessionId(session.id)

        logger.info("Start cover letter generation: sessionId={}, accountId={}, traceId={}", session.id, cmd.accountId, cmd.traceId)

        return runCatching {
            val content = generateWithLlm(
                company = session.company,
                position = session.position,
                jobDescription = session.jobDescription,
                resumeText = resumeText,
                keyRequirements = advice?.keyRequirements.orEmpty(),
                gaps = advice?.gaps.orEmpty(),
            )
            val version = (sessionCoverLetterRepo.selectMaxVersionBySessionId(session.id) ?: 0) + 1
            sessionCoverLetterRepo.insertAndReturn(
                SessionCoverLetterInsertQuery(
                    sessionId = session.id,
                    version = version,
                    content = content,
                ),
            )
        }.onSuccess {
            logger.info(
                "Cover letter generated: sessionId={}, version={}, coverLetterId={}, traceId={}",
                session.id,
                it.version,
                it.id,
                cmd.traceId,
            )
        }.onFailure { error ->
            logger.error("Cover letter generation failed: sessionId={}, traceId={}, error={}", session.id, cmd.traceId, error.message, error)
        }.getOrElse {
            throw AppException(message = "failed to generate cover letter", cause = it)
        }
    }

    override fun get(cmd: SessionCoverLetterGetCmd): SessionCoverLetter {
        val session = applicationSessionRepo.selectById(cmd.sessionId)
            ?: throw NotFoundException("application session not found")
        if (session.accountId != cmd.accountId) {
            throw NotFoundException("application session not found")
        }
        val coverLetter = cmd.version
            ?.let { sessionCoverLetterRepo.selectBySessionIdAndVersion(session.id, it) }
            ?: sessionCoverLetterRepo.selectLatestBySessionId(session.id)
        return coverLetter ?: throw NotFoundException("session cover letter not found")
    }

    private fun generateWithLlm(
        company: String,
        position: String,
        jobDescription: String,
        resumeText: String,
        keyRequirements: List<String>,
        gaps: List<String>,
    ): String {
        val systemPrompt = readText(systemPromptResource)
        val userPrompt = readText(userPromptResource)
            .replace("\$company\$", company)
            .replace("\$position\$", position)
            .replace("\$jobDescription\$", jobDescription)
            .replace("\$resumeText\$", resumeText.ifBlank { "Not provided" })
            .replace("\$keyRequirements\$", keyRequirements.joinToString("\n") { "- $it" }.ifBlank { "- Not available" })
            .replace("\$gaps\$", gaps.joinToString("\n") { "- $it" }.ifBlank { "- Not available" })
        val raw = llmClient.chat(systemPrompt, userPrompt)
        val normalized = normalizeMarkdown(raw)
        if (normalized.isBlank()) {
            throw AppException(message = "cover letter content is empty")
        }
        return normalized.take(MAX_CONTENT_LENGTH)
    }

    private fun normalizeMarkdown(content: String): String {
        val trimmed = content.trim()
        val fenced = Regex("^```(?:markdown|md)?\\s*([\\s\\S]*?)\\s*```$", RegexOption.IGNORE_CASE)
        return fenced.find(trimmed)?.groupValues?.get(1)?.trim() ?: trimmed
    }

    private fun readText(resource: Resource): String =
        resource.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
