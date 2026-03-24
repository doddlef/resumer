package dev.haomin.resumer.app.module.session.service.impl

import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.session.domain.ApplicationSession
import dev.haomin.resumer.app.module.session.domain.TaskStatus
import dev.haomin.resumer.app.module.session.mq.SessionAdvicePublisher
import dev.haomin.resumer.app.module.session.mq.model.AdviceJobType
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.service.dto.QueueSessionAdviceTaskCmd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SessionAdviceTaskServiceImplTest {

    @Mock
    private lateinit var applicationSessionRepo: ApplicationSessionRepo

    @Mock
    private lateinit var publisher: SessionAdvicePublisher

    @Test
    fun `queue should set pending and publish for position advice`() {
        val service = SessionAdviceTaskServiceImpl(applicationSessionRepo, publisher)
        val session = session()
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)
        whenever(publisher.publish(eq(session.id), eq(session.accountId), eq(AdviceJobType.POSITION_ADVICE), any()))
            .thenReturn("msg-id")

        val result = service.queue(
            QueueSessionAdviceTaskCmd(
                sessionId = session.id,
                accountId = session.accountId,
                jobType = AdviceJobType.POSITION_ADVICE,
            ),
        )

        assertEquals(session.id, result.sessionId)
        assertEquals(AdviceJobType.POSITION_ADVICE, result.jobType)
        assertEquals(TaskStatus.PENDING, result.status)
        verify(applicationSessionRepo, times(1)).updateById(eq(session.id), any())
        verify(publisher, times(1)).publish(eq(session.id), eq(session.accountId), eq(AdviceJobType.POSITION_ADVICE), any())
    }

    @Test
    fun `queue should throw not found when ownership mismatched`() {
        val service = SessionAdviceTaskServiceImpl(applicationSessionRepo, publisher)
        val session = session()
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)

        assertThrows(NotFoundException::class.java) {
            service.queue(
                QueueSessionAdviceTaskCmd(
                    sessionId = session.id,
                    accountId = UUID.randomUUID(),
                    jobType = AdviceJobType.RESUME_FIT,
                ),
            )
        }
    }

    @Test
    fun `queue should mark failed and throw when publish fails`() {
        val service = SessionAdviceTaskServiceImpl(applicationSessionRepo, publisher)
        val session = session()
        whenever(applicationSessionRepo.selectById(eq(session.id))).thenReturn(session)
        whenever(publisher.publish(eq(session.id), eq(session.accountId), eq(AdviceJobType.RESUME_FIT), any()))
            .thenThrow(RuntimeException("redis down"))

        assertThrows(AppException::class.java) {
            service.queue(
                QueueSessionAdviceTaskCmd(
                    sessionId = session.id,
                    accountId = session.accountId,
                    jobType = AdviceJobType.RESUME_FIT,
                ),
            )
        }

        verify(applicationSessionRepo, times(2)).updateById(eq(session.id), any())
    }

    private fun session(): ApplicationSession {
        val now = OffsetDateTime.now()
        return ApplicationSession(
            id = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            resumeId = null,
            company = "OpenAI",
            position = "Backend Engineer",
            jobDescription = "Build services",
            createdAt = now,
            updatedAt = now,
            positionAdviceStatus = TaskStatus.PENDING,
            positionAdviceError = null,
            resumeFitStatus = TaskStatus.PENDING,
            resumeFitError = null,
        )
    }
}
