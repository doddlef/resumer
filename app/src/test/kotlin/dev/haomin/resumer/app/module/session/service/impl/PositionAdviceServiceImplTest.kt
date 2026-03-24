package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.ai.LLMClient
import dev.haomin.resumer.app.common.ai.StructuredOutputInvoker
import dev.haomin.resumer.app.common.ai.StructuredOutputProperties
import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.session.domain.ApplicationSession
import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.domain.TaskStatus
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.SessionAdviceRepo
import dev.haomin.resumer.app.module.session.repo.query.SessionAdviceInsertQuery
import dev.haomin.resumer.app.module.session.service.dto.PositionAdviceGenerateCmd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.io.ByteArrayResource
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PositionAdviceServiceImplTest {

    @Mock
    private lateinit var applicationSessionRepo: ApplicationSessionRepo

    @Mock
    private lateinit var sessionAdviceRepo: SessionAdviceRepo

    @Mock
    private lateinit var llmClient: LLMClient

    @Test
    fun `generate should persist normalized position advice`() {
        val service = service()
        val session = session()
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)
        whenever(llmClient.chat(any(), any())).thenReturn(
            """
            {
              "summary": " Role has strong backend focus. ",
              "keyRequirements": [" Spring Boot ", "", "Redis"],
              "likelyInterviewFocus": ["System design", "API design"],
              "redFlags": ["No internship experience"]
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
            PositionAdviceGenerateCmd(
                sessionId = session.id,
                accountId = session.accountId,
                traceId = "trace-1",
            ),
        )

        assertEquals("Role has strong backend focus.", result.positionSummary)
        assertEquals(listOf("Spring Boot", "Redis"), result.keyRequirements)
        assertEquals(0, result.fitScore)
    }

    @Test
    fun `generate should throw not found when session belongs to another account`() {
        val service = service()
        val session = session()
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)

        assertThrows(NotFoundException::class.java) {
            service.generate(
                PositionAdviceGenerateCmd(
                    sessionId = session.id,
                    accountId = UUID.randomUUID(),
                    traceId = "trace-2",
                ),
            )
        }

        verify(sessionAdviceRepo, never()).insertAndReturn(any())
    }

    @Test
    fun `generate should throw AppException when llm call fails`() {
        val service = service()
        val session = session()
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)
        whenever(llmClient.chat(any(), any())).thenThrow(RuntimeException("model unavailable"))

        assertThrows(AppException::class.java) {
            service.generate(
                PositionAdviceGenerateCmd(
                    sessionId = session.id,
                    accountId = session.accountId,
                    traceId = "trace-3",
                ),
            )
        }
    }

    private fun service(): PositionAdviceServiceImpl {
        val objectMapper = jacksonObjectMapper()
        val invoker = StructuredOutputInvoker(
            StructuredOutputProperties(
                maxAttempts = 1,
                includeLastErrorInRetryPrompt = true,
                maxErrorMessageLength = 200,
            ),
        )
        return PositionAdviceServiceImpl(
            applicationSessionRepo = applicationSessionRepo,
            sessionAdviceRepo = sessionAdviceRepo,
            llmClient = llmClient,
            structuredOutputInvoker = invoker,
            objectMapper = objectMapper,
            systemPromptResource = ByteArrayResource("system prompt".toByteArray()),
            userPromptResource = ByteArrayResource(
                """
                Company: ${'$'}company${'$'}
                Position: ${'$'}position${'$'}
                Job Description: ${'$'}jobDescription${'$'}
                """.trimIndent().toByteArray(),
            ),
        )
    }

    private fun session(): ApplicationSession {
        val now = OffsetDateTime.now()
        return ApplicationSession(
            id = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            resumeId = null,
            company = "OpenAI",
            position = "Backend Engineer",
            jobDescription = "Build scalable services",
            createdAt = now,
            updatedAt = now,
            positionAdviceStatus = TaskStatus.PENDING,
            positionAdviceError = null,
            resumeFitStatus = TaskStatus.PENDING,
            resumeFitError = null,
        )
    }
}
