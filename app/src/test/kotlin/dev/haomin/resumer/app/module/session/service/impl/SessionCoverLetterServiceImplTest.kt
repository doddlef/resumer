package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.session.domain.ApplicationSession
import dev.haomin.resumer.app.module.session.domain.RewriteSuggestion
import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.domain.SessionCoverLetter
import dev.haomin.resumer.app.module.session.domain.TaskStatus
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.SessionAdviceRepo
import dev.haomin.resumer.app.module.session.repo.SessionCoverLetterRepo
import dev.haomin.resumer.app.module.session.repo.query.SessionCoverLetterInsertQuery
import dev.haomin.resumer.app.module.session.service.dto.SessionCoverLetterGenerateCmd
import dev.haomin.resumer.app.module.session.service.dto.SessionCoverLetterGetCmd
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
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SessionCoverLetterServiceImplTest {

    @Mock
    private lateinit var applicationSessionRepo: ApplicationSessionRepo

    @Mock
    private lateinit var resumeRepo: ResumeRepo

    @Mock
    private lateinit var sessionAdviceRepo: SessionAdviceRepo

    @Mock
    private lateinit var sessionCoverLetterRepo: SessionCoverLetterRepo

    @Mock
    private lateinit var llmClient: dev.haomin.resumer.app.common.ai.LLMClient

    @Test
    fun `generate should create next version and strip fenced markdown`() {
        val service = service()
        val session = session()
        val resume = resume(session)
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)
        whenever(resumeRepo.selectById(eq(requireNotNull(session.resumeId)))).thenReturn(resume)
        whenever(sessionAdviceRepo.selectLatestBySessionId(eq(session.id))).thenReturn(advice(session.id))
        whenever(sessionCoverLetterRepo.selectMaxVersionBySessionId(eq(session.id))).thenReturn(2)
        whenever(llmClient.chat(any(), any())).thenReturn(
            """
            ```markdown
            Dear Hiring Manager,

            I am excited to apply.
            ```
            """.trimIndent(),
        )
        whenever(sessionCoverLetterRepo.insertAndReturn(any())).thenAnswer { invocation ->
            val q = invocation.getArgument<SessionCoverLetterInsertQuery>(0)
            SessionCoverLetter(
                id = q.id,
                sessionId = q.sessionId,
                version = q.version,
                content = q.content,
                createdAt = q.createdAt,
            )
        }

        val result = service.generate(
            SessionCoverLetterGenerateCmd(
                sessionId = session.id,
                accountId = session.accountId,
                traceId = "trace-cl-1",
            ),
        )

        assertEquals(3, result.version)
        assertEquals("Dear Hiring Manager,\n\nI am excited to apply.", result.content)
    }

    @Test
    fun `get should return latest when version is null`() {
        val service = service()
        val session = session()
        val letter = SessionCoverLetter(
            id = UUID.randomUUID(),
            sessionId = session.id,
            version = 1,
            content = "content",
            createdAt = OffsetDateTime.now(),
        )
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)
        whenever(sessionCoverLetterRepo.selectLatestBySessionId(eq(session.id))).thenReturn(letter)

        val result = service.get(SessionCoverLetterGetCmd(sessionId = session.id, accountId = session.accountId))
        assertEquals(1, result.version)
    }

    @Test
    fun `get should throw when session belongs to another account`() {
        val service = service()
        val session = session()
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)

        assertThrows(NotFoundException::class.java) {
            service.get(
                SessionCoverLetterGetCmd(
                    sessionId = session.id,
                    accountId = UUID.randomUUID(),
                    version = 1,
                ),
            )
        }
    }

    private fun service(): SessionCoverLetterServiceImpl =
        SessionCoverLetterServiceImpl(
            applicationSessionRepo = applicationSessionRepo,
            resumeRepo = resumeRepo,
            sessionAdviceRepo = sessionAdviceRepo,
            sessionCoverLetterRepo = sessionCoverLetterRepo,
            llmClient = llmClient,
            systemPromptResource = ByteArrayResource("system".toByteArray()),
            userPromptResource = ByteArrayResource(
                """
                Company: ${'$'}company${'$'}
                Position: ${'$'}position${'$'}
                ${'$'}resumeText${'$'}
                """.trimIndent().toByteArray(),
            ),
        )

    private fun session(): ApplicationSession {
        val now = OffsetDateTime.now()
        return ApplicationSession(
            id = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            resumeId = UUID.randomUUID(),
            company = "OpenAI",
            position = "Backend Engineer",
            jobDescription = "Build APIs",
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
            content = "Spring Boot Kotlin Redis",
            fileHash = "hash",
            size = 1000,
            storageKey = "resume/key",
            status = ResumeStatus.COMPLETED,
            error = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun advice(sessionId: UUID): SessionAdvice =
        SessionAdvice(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            positionSummary = "summary",
            keyRequirements = listOf("Spring Boot"),
            likelyInterviewFocus = listOf("API"),
            redFlags = emptyList(),
            fitScore = 70,
            gaps = listOf("No production metrics"),
            rewriteSuggestions = emptyList<RewriteSuggestion>(),
            createdAt = OffsetDateTime.now(),
        )
}
