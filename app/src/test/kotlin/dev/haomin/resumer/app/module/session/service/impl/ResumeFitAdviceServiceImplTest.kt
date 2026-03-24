package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.ai.LLMClient
import dev.haomin.resumer.app.common.ai.StructuredOutputInvoker
import dev.haomin.resumer.app.common.ai.StructuredOutputProperties
import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.session.domain.ApplicationSession
import dev.haomin.resumer.app.module.session.domain.RewriteSuggestion
import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.domain.TaskStatus
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.SessionAdviceRepo
import dev.haomin.resumer.app.module.session.repo.query.SessionAdviceInsertQuery
import dev.haomin.resumer.app.module.session.service.dto.ResumeFitAdviceGenerateCmd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.core.io.ByteArrayResource
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ResumeFitAdviceServiceImplTest {

    @Mock
    private lateinit var applicationSessionRepo: ApplicationSessionRepo

    @Mock
    private lateinit var resumeRepo: ResumeRepo

    @Mock
    private lateinit var sessionAdviceRepo: SessionAdviceRepo

    @Mock
    private lateinit var llmClient: LLMClient

    @Test
    fun `generate should persist normalized resume fit advice`() {
        val service = service()
        val session = session()
        val resume = resume(session)
        val latest = latestAdvice(session.id)
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)
        whenever(sessionAdviceRepo.selectLatestBySessionId(eq(session.id))).thenReturn(latest)
        whenever(llmClient.chat(any(), any())).thenReturn(
            """
            {
              "fitScore": 89,
              "gaps": [" Missing measurable outcomes ", ""],
              "rewriteSuggestions": [
                {
                  "section": "experience",
                  "priority": "HIGH",
                  "issue": "bullet is generic",
                  "recommendation": "add metrics",
                  "example": "Improved API throughput by 28%."
                }
              ]
            }
            """.trimIndent(),
        )
        whenever(sessionAdviceRepo.insertAndReturn(any())).thenAnswer { invocation ->
            val q = invocation.getArgument<SessionAdviceInsertQuery>(0)
            SessionAdvice(
                id = q.id,
                sessionId = q.sessionId,
                positionSummary = q.positionSummary,
                keyRequirements = q.keyRequirements,
                likelyInterviewFocus = q.likelyInterviewFocus,
                redFlags = q.redFlags,
                fitScore = q.fitScore,
                gaps = q.gaps,
                rewriteSuggestions = q.rewriteSuggestions,
                createdAt = q.createdAt,
            )
        }

        val result = service.generate(
            ResumeFitAdviceGenerateCmd(
                sessionId = session.id,
                accountId = session.accountId,
                traceId = "trace-fit-1",
            ),
        )

        assertEquals(89, result.fitScore)
        assertEquals(listOf("Missing measurable outcomes"), result.gaps)
        assertEquals(1, result.rewriteSuggestions.size)
        assertEquals("high", result.rewriteSuggestions.first().priority)
        assertEquals(latest.positionSummary, result.positionSummary)
    }

    @Test
    fun `generate should fail when session has no resume id`() {
        val service = service()
        val session = session().copy(resumeId = null)
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)

        assertThrows(InvalidParamException::class.java) {
            service.generate(
                ResumeFitAdviceGenerateCmd(
                    sessionId = session.id,
                    accountId = session.accountId,
                    traceId = "trace-fit-2",
                ),
            )
        }
    }

    private fun service(): ResumeFitAdviceServiceImpl {
        val invoker = StructuredOutputInvoker(
            StructuredOutputProperties(
                maxAttempts = 1,
                includeLastErrorInRetryPrompt = true,
                maxErrorMessageLength = 200,
            ),
        )
        return ResumeFitAdviceServiceImpl(
            applicationSessionRepo = applicationSessionRepo,
            resumeRepo = resumeRepo,
            sessionAdviceRepo = sessionAdviceRepo,
            llmClient = llmClient,
            structuredOutputInvoker = invoker,
            objectMapper = jacksonObjectMapper(),
            systemPromptResource = ByteArrayResource("system prompt".toByteArray()),
            userPromptResource = ByteArrayResource(
                """
                Company: ${'$'}company${'$'}
                Position: ${'$'}position${'$'}
                Requirements:
                ${'$'}keyRequirements${'$'}
                Resume:
                ${'$'}resumeText${'$'}
                """.trimIndent().toByteArray(),
            ),
        )
    }

    private fun session(): ApplicationSession {
        val now = OffsetDateTime.now()
        return ApplicationSession(
            id = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            resumeId = UUID.randomUUID(),
            company = "OpenAI",
            position = "Backend Engineer",
            jobDescription = "Build and maintain APIs",
            createdAt = now,
            updatedAt = now,
            positionAdviceStatus = TaskStatus.PENDING,
            positionAdviceError = null,
            resumeFitStatus = TaskStatus.PENDING,
            resumeFitError = null,
        )
    }

    private fun resume(session: ApplicationSession): Resume {
        val now = OffsetDateTime.now()
        return Resume(
            id = requireNotNull(session.resumeId),
            accountId = session.accountId,
            filename = "resume.pdf",
            content = "Java Spring Boot Redis experience",
            fileHash = "hash",
            size = 1000,
            storageKey = "resume/key.pdf",
            status = ResumeStatus.COMPLETED,
            error = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun latestAdvice(sessionId: UUID): SessionAdvice {
        return SessionAdvice(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            positionSummary = "Strong backend role with API ownership.",
            keyRequirements = listOf("Spring Boot", "SQL"),
            likelyInterviewFocus = listOf("API design"),
            redFlags = listOf("Weak project impact"),
            fitScore = 0,
            gaps = emptyList(),
            rewriteSuggestions = emptyList<RewriteSuggestion>(),
            createdAt = OffsetDateTime.now(),
        )
    }
}
