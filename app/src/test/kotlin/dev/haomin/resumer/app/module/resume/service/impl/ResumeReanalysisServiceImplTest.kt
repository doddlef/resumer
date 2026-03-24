package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.mq.ResumeAnalyzePublisher
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.resume.service.dto.ResumeReanalyzeCmd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ResumeReanalysisServiceImplTest {

    @Mock
    private lateinit var resumeRepo: ResumeRepo

    @Mock
    private lateinit var publisher: ResumeAnalyzePublisher

    @Test
    fun `reanalyze should check ownership and queue job`() {
        val service = ResumeReanalysisServiceImpl(resumeRepo, publisher)
        val resume = resume()
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)

        val result = service.reanalyze(
            ResumeReanalyzeCmd(
                resumeId = resume.id,
                accountId = resume.accountId,
            ),
        )

        assertEquals(resume.id, result.resumeId)
        assertEquals(ResumeStatus.PENDING, result.status)
        verify(resumeRepo).updateById(eq(resume.id), any())
        verify(publisher).publishResumeAnalyze(eq(resume.id), eq(resume.accountId), eq(result.traceId))
    }

    @Test
    fun `reanalyze should throw not found when resume belongs to another user`() {
        val service = ResumeReanalysisServiceImpl(resumeRepo, publisher)
        val resume = resume()
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)

        assertThrows(NotFoundException::class.java) {
            service.reanalyze(
                ResumeReanalyzeCmd(
                    resumeId = resume.id,
                    accountId = UUID.randomUUID(),
                ),
            )
        }

        verify(resumeRepo, never()).updateById(any(), any())
        verify(publisher, never()).publishResumeAnalyze(any(), any(), any())
    }

    @Test
    fun `reanalyze should throw invalid param when content is empty`() {
        val service = ResumeReanalysisServiceImpl(resumeRepo, publisher)
        val resume = resume(content = " ")
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)

        assertThrows(InvalidParamException::class.java) {
            service.reanalyze(
                ResumeReanalyzeCmd(
                    resumeId = resume.id,
                    accountId = resume.accountId,
                ),
            )
        }

        verify(resumeRepo, never()).updateById(any(), any())
        verify(publisher, never()).publishResumeAnalyze(any(), any(), any())
    }

    @Test
    fun `reanalyze should mark failed when publish throws`() {
        val service = ResumeReanalysisServiceImpl(resumeRepo, publisher)
        val resume = resume()
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)
        whenever(publisher.publishResumeAnalyze(eq(resume.id), eq(resume.accountId), any())).thenThrow(RuntimeException("redis down"))

        assertThrows(AppException::class.java) {
            service.reanalyze(
                ResumeReanalyzeCmd(
                    resumeId = resume.id,
                    accountId = resume.accountId,
                ),
            )
        }

        val updateCaptor = argumentCaptor<dev.haomin.resumer.app.module.resume.repo.query.ResumeUpdateQuery>()
        verify(resumeRepo, times(2)).updateById(eq(resume.id), updateCaptor.capture())
        assertEquals(ResumeStatus.PENDING, updateCaptor.allValues[0].status)
        assertEquals(ResumeStatus.FAILED, updateCaptor.allValues[1].status)
    }

    private fun resume(content: String? = "resume content"): Resume {
        val now = OffsetDateTime.now()
        return Resume(
            id = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            filename = "resume.pdf",
            content = content,
            fileHash = "hash",
            size = 1024,
            storageKey = "resumes/x/resume.pdf",
            status = ResumeStatus.COMPLETED,
            error = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}
