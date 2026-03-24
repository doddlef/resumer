package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.common.ai.LLMClient
import dev.haomin.resumer.app.common.ai.StructuredOutputInvoker
import dev.haomin.resumer.app.common.ai.StructuredOutputProperties
import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.domain.ResumeAnalysis
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.repo.ResumeAnalysisRepo
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.resume.repo.query.ResumeAnalysisInsertQuery
import dev.haomin.resumer.app.module.resume.repo.query.ResumeUpdateQuery
import dev.haomin.resumer.app.module.resume.service.dto.ResumeAnalyzeCmd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.io.ByteArrayResource
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ResumeAnalysisServiceImplTest {

    @Mock
    private lateinit var resumeRepo: ResumeRepo

    @Mock
    private lateinit var resumeAnalysisRepo: ResumeAnalysisRepo

    @Mock
    private lateinit var llmClient: LLMClient

    private lateinit var service: ResumeAnalysisServiceImpl

    @BeforeEach
    fun setUp() {
        val objectMapper = jacksonObjectMapper()
        val invoker = StructuredOutputInvoker(
            StructuredOutputProperties(
                maxAttempts = 1,
                includeLastErrorInRetryPrompt = true,
                maxErrorMessageLength = 200,
            ),
        )

        service = ResumeAnalysisServiceImpl(
            resumeRepo = resumeRepo,
            resumeAnalysisRepo = resumeAnalysisRepo,
            llmClient = llmClient,
            structuredOutputInvoker = invoker,
            objectMapper = objectMapper,
            systemPromptResource = ByteArrayResource("system prompt".toByteArray()),
            userPromptResource = ByteArrayResource("resume:\n\$resumeText\$".toByteArray()),
        )
    }

    @Test
    fun `analyze should save analysis and mark completed on success`() {
        val resume = resume(content = "Experienced backend engineer")
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)
        whenever(llmClient.chat(any(), any())).thenReturn(
            """
            {
              "score": 120,
              "summary": "Strong foundation but impact statements can be improved.",
              "strengths": ["  Strong backend development  ", "", "Spring experience"],
              "suggestions": [
                {
                  "category": "unknown",
                  "priority": "urgent",
                  "issue": "Project bullets are vague",
                  "recommendation": "Original: Built API. Improved: Designed and implemented REST APIs with JWT auth and refresh rotation."
                }
              ]
            }
            """.trimIndent(),
        )
        whenever(resumeAnalysisRepo.insertAndReturn(any())).thenAnswer { invocation ->
            val q = invocation.getArgument<ResumeAnalysisInsertQuery>(0)
            ResumeAnalysis(
                id = q.id,
                resumeId = q.resumeId,
                score = q.score,
                summary = q.summary,
                strengths = q.strengths,
                suggestions = q.suggestions,
                createdAt = q.createdAt,
            )
        }

        val result = service.analyze(
            ResumeAnalyzeCmd(
                resumeId = resume.id,
                accountId = resume.accountId,
                traceId = "trace-1",
            ),
        )

        assertEquals(100, result.score)
        assertEquals(2, result.strengths.size)
        assertEquals("content", result.suggestions.first().category)
        assertEquals("medium", result.suggestions.first().priority)

        val updateCaptor = argumentCaptor<ResumeUpdateQuery>()
        verify(resumeRepo, times(2)).updateById(eq(resume.id), updateCaptor.capture())
        assertEquals(ResumeStatus.ANALYZING, updateCaptor.allValues[0].status)
        assertEquals(ResumeStatus.COMPLETED, updateCaptor.allValues[1].status)
    }

    @Test
    fun `analyze should throw when resume not found`() {
        val resumeId = UUID.randomUUID()
        whenever(resumeRepo.selectById(eq(resumeId))).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            service.analyze(
                ResumeAnalyzeCmd(
                    resumeId = resumeId,
                    accountId = UUID.randomUUID(),
                    traceId = "trace-2",
                ),
            )
        }

        verify(resumeRepo, never()).updateById(any(), any())
        verify(resumeAnalysisRepo, never()).insertAndReturn(any())
    }

    @Test
    fun `analyze should throw when account mismatch`() {
        val resume = resume()
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)

        assertThrows(InvalidParamException::class.java) {
            service.analyze(
                ResumeAnalyzeCmd(
                    resumeId = resume.id,
                    accountId = UUID.randomUUID(),
                    traceId = "trace-3",
                ),
            )
        }

        verify(resumeRepo, never()).updateById(any(), any())
        verify(resumeAnalysisRepo, never()).insertAndReturn(any())
    }

    @Test
    fun `analyze should mark failed when resume content is empty`() {
        val resume = resume(content = "  ")
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)

        assertThrows(InvalidParamException::class.java) {
            service.analyze(
                ResumeAnalyzeCmd(
                    resumeId = resume.id,
                    accountId = resume.accountId,
                    traceId = "trace-4",
                ),
            )
        }

        val updateCaptor = argumentCaptor<ResumeUpdateQuery>()
        verify(resumeRepo).updateById(eq(resume.id), updateCaptor.capture())
        assertEquals(ResumeStatus.FAILED, updateCaptor.firstValue.status)
    }

    @Test
    fun `analyze should mark failed and throw AppException when llm call fails`() {
        val resume = resume(content = "Kotlin Spring Boot project")
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)
        whenever(llmClient.chat(any(), any())).thenThrow(RuntimeException("model unavailable"))

        assertThrows(AppException::class.java) {
            service.analyze(
                ResumeAnalyzeCmd(
                    resumeId = resume.id,
                    accountId = resume.accountId,
                    traceId = "trace-5",
                ),
            )
        }

        val updateCaptor = argumentCaptor<ResumeUpdateQuery>()
        verify(resumeRepo, times(2)).updateById(eq(resume.id), updateCaptor.capture())
        assertEquals(ResumeStatus.ANALYZING, updateCaptor.allValues[0].status)
        assertEquals(ResumeStatus.FAILED, updateCaptor.allValues[1].status)
        verify(resumeAnalysisRepo, never()).insertAndReturn(any())
    }

    private fun resume(content: String? = "resume content"): Resume {
        val now = OffsetDateTime.now()
        return Resume(
            id = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            filename = "resume.pdf",
            content = content,
            fileHash = "hash",
            size = 1234,
            storageKey = "resumes/test.pdf",
            status = ResumeStatus.PENDING,
            error = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}
