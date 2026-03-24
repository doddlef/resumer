package dev.haomin.resumer.app.module.session.mq

import dev.haomin.resumer.app.module.session.mq.model.AdviceJobType
import dev.haomin.resumer.app.module.session.mq.model.SessionAdviceJobPayload
import dev.haomin.resumer.app.module.session.repo.ApplicationSessionRepo
import dev.haomin.resumer.app.module.session.service.PositionAdviceService
import dev.haomin.resumer.app.module.session.service.ResumeFitAdviceService
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
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SessionAdviceJobProcessorImplTest {

    @Mock
    private lateinit var applicationSessionRepo: ApplicationSessionRepo

    @Mock
    private lateinit var positionAdviceService: PositionAdviceService

    @Mock
    private lateinit var resumeFitAdviceService: ResumeFitAdviceService

    @Test
    fun `process should call position advice service and update status`() {
        val processor = SessionAdviceJobProcessorImpl(applicationSessionRepo, positionAdviceService, resumeFitAdviceService)
        val payload = SessionAdviceJobPayload(
            sessionId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            jobType = AdviceJobType.POSITION_ADVICE,
        )

        processor.process(payload)

        verify(applicationSessionRepo, times(2)).updateById(eq(payload.sessionId), any())
        verify(positionAdviceService, times(1)).generate(any())
    }

    @Test
    fun `process should call resume fit service and update status`() {
        val processor = SessionAdviceJobProcessorImpl(applicationSessionRepo, positionAdviceService, resumeFitAdviceService)
        val payload = SessionAdviceJobPayload(
            sessionId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            jobType = AdviceJobType.RESUME_FIT,
        )

        processor.process(payload)

        verify(applicationSessionRepo, times(2)).updateById(eq(payload.sessionId), any())
        verify(resumeFitAdviceService, times(1)).generate(any())
    }

    @Test
    fun `process should mark failed when position advice service throws`() {
        val processor = SessionAdviceJobProcessorImpl(applicationSessionRepo, positionAdviceService, resumeFitAdviceService)
        val payload = SessionAdviceJobPayload(
            sessionId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            jobType = AdviceJobType.POSITION_ADVICE,
        )
        whenever(positionAdviceService.generate(any())).thenThrow(RuntimeException("llm failed"))

        assertThrows(RuntimeException::class.java) {
            processor.process(payload)
        }
        verify(applicationSessionRepo, times(2)).updateById(eq(payload.sessionId), any())
    }

    @Test
    fun `process should mark failed when resume fit service throws`() {
        val processor = SessionAdviceJobProcessorImpl(applicationSessionRepo, positionAdviceService, resumeFitAdviceService)
        val payload = SessionAdviceJobPayload(
            sessionId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            jobType = AdviceJobType.RESUME_FIT,
        )
        whenever(resumeFitAdviceService.generate(any())).thenThrow(RuntimeException("llm failed"))

        assertThrows(RuntimeException::class.java) {
            processor.process(payload)
        }
        verify(applicationSessionRepo, times(2)).updateById(eq(payload.sessionId), any())
    }
}
