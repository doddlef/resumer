package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.session.domain.ApplicationSession
import dev.haomin.resumer.app.module.session.domain.TaskStatus
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.repo.query.ApplicationSessionInsertQuery
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionCreateCmd
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionDetailCmd
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionListCmd
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
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ApplicationSessionServiceImplTest {

    @Mock
    private lateinit var applicationSessionRepo: ApplicationSessionRepo

    @Mock
    private lateinit var resumeRepo: ResumeRepo

    @Test
    fun `create should validate resume ownership and persist session`() {
        val service = ApplicationSessionServiceImpl(applicationSessionRepo, resumeRepo)
        val accountId = UUID.randomUUID()
        val resume = resume(accountId)
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)
        whenever(applicationSessionRepo.insertAndReturn(any())).thenAnswer { invocation ->
            val q = invocation.getArgument<ApplicationSessionInsertQuery>(0)
            ApplicationSession(
                id = q.id,
                accountId = q.accountId,
                resumeId = q.resumeId,
                company = q.company,
                position = q.position,
                jobDescription = q.jobDescription,
            createdAt = q.createdAt,
            updatedAt = q.updatedAt,
            positionAdviceStatus = TaskStatus.PENDING,
            positionAdviceError = null,
            resumeFitStatus = TaskStatus.PENDING,
            resumeFitError = null,
        )
        }

        val result = service.create(
            ApplicationSessionCreateCmd(
                accountId = accountId,
                resumeId = resume.id,
                company = " OpenAI ",
                position = " Backend Engineer ",
                jobDescription = " Build APIs ",
            ),
        )

        assertEquals(resume.id, result.resumeId)
        assertEquals("OpenAI", result.company)
        assertEquals("Backend Engineer", result.position)
        assertEquals("Build APIs", result.jobDescription)
    }

    @Test
    fun `create should throw not found when resume belongs to another account`() {
        val service = ApplicationSessionServiceImpl(applicationSessionRepo, resumeRepo)
        val ownerId = UUID.randomUUID()
        val callerId = UUID.randomUUID()
        val resume = resume(ownerId)
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)

        assertThrows(NotFoundException::class.java) {
            service.create(
                ApplicationSessionCreateCmd(
                    accountId = callerId,
                    resumeId = resume.id,
                    company = "OpenAI",
                    position = "Backend Engineer",
                    jobDescription = "Build APIs",
                ),
            )
        }

        verify(applicationSessionRepo, never()).insertAndReturn(any<ApplicationSessionInsertQuery>())
    }

    @Test
    fun `list should return sessions by account`() {
        val service = ApplicationSessionServiceImpl(applicationSessionRepo, resumeRepo)
        val accountId = UUID.randomUUID()
        val sessions = listOf(
            session(accountId = accountId, company = "A", position = "P1"),
            session(accountId = accountId, company = "B", position = "P2"),
        )
        whenever(applicationSessionRepo.selectByAccountId(eq(accountId))).thenReturn(sessions)

        val result = service.list(ApplicationSessionListCmd(accountId))

        assertEquals(2, result.sessions.size)
        assertEquals("A", result.sessions[0].company)
        assertEquals("B", result.sessions[1].company)
    }

    @Test
    fun `detail should throw not found when session belongs to another account`() {
        val service = ApplicationSessionServiceImpl(applicationSessionRepo, resumeRepo)
        val session = session(accountId = UUID.randomUUID())
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)

        assertThrows(NotFoundException::class.java) {
            service.detail(
                ApplicationSessionDetailCmd(
                    sessionId = session.id,
                    accountId = UUID.randomUUID(),
                ),
            )
        }
    }

    private fun session(
        accountId: UUID,
        resumeId: UUID? = null,
        company: String = "OpenAI",
        position: String = "Backend Engineer",
    ): ApplicationSession {
        val now = OffsetDateTime.now()
        return ApplicationSession(
            id = UUID.randomUUID(),
            accountId = accountId,
            resumeId = resumeId,
            company = company,
            position = position,
            jobDescription = "Job description",
            createdAt = now,
            updatedAt = now,
            positionAdviceStatus = TaskStatus.PENDING,
            positionAdviceError = null,
            resumeFitStatus = TaskStatus.PENDING,
            resumeFitError = null,
        )
    }

    private fun resume(accountId: UUID): Resume {
        val now = OffsetDateTime.now()
        return Resume(
            id = UUID.randomUUID(),
            accountId = accountId,
            filename = "resume.pdf",
            content = "resume content",
            fileHash = "hash",
            size = 123,
            storageKey = "resumes/key",
            status = ResumeStatus.COMPLETED,
            error = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}
