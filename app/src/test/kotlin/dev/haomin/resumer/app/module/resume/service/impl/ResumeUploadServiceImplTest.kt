package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.mq.ResumeAnalyzePublisher
import dev.haomin.resumer.app.module.resume.service.dto.ResumeUploadCmd
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
import org.springframework.mock.web.MockMultipartFile
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ResumeUploadServiceImplTest {

    @Mock
    private lateinit var parseService: ResumeParseService

    @Mock
    private lateinit var storageService: ResumeStorageService

    @Mock
    private lateinit var validateService: ResumeValidationService

    @Mock
    private lateinit var analysisPublisher: ResumeAnalyzePublisher

    @Test
    fun `uploadAndAnalyze should return duplicate when same hash exists`() {
        val service = ResumeUploadServiceImpl(parseService, storageService, validateService, analysisPublisher)
        val accountId = UUID.randomUUID()
        val existing = resume(
            accountId = accountId,
            filename = "old.pdf",
            status = ResumeStatus.COMPLETED,
        )
        whenever(parseService.detectContentType(any())).thenReturn("application/pdf")
        whenever(storageService.calculateContentHash(any())).thenReturn("hash-a")
        whenever(storageService.findExistingResume(eq("hash-a"), eq(accountId))).thenReturn(existing)

        val result = service.uploadAndAnalyze(
            ResumeUploadCmd(
                file = MockMultipartFile(
                    "file",
                    "resume.pdf",
                    "application/pdf",
                    "pdf".toByteArray(),
                ),
                accountId = accountId,
            ),
        )

        assertEquals(existing.id, result.resumeId)
        assertEquals(true, result.duplicate)
        verify(parseService, never()).parseResume(any())
        verify(storageService, never()).uploadResume(any(), any())
        verify(analysisPublisher, never()).publishResumeAnalyze(any(), any(), any())
    }

    @Test
    fun `uploadAndAnalyze should mark failed when publish throws`() {
        val service = ResumeUploadServiceImpl(parseService, storageService, validateService, analysisPublisher)
        val accountId = UUID.randomUUID()
        val saved = resume(accountId = accountId, filename = "new.pdf", status = ResumeStatus.PENDING)
        whenever(parseService.detectContentType(any())).thenReturn("application/pdf")
        whenever(storageService.calculateContentHash(any())).thenReturn("hash-b")
        whenever(storageService.findExistingResume(eq("hash-b"), eq(accountId))).thenReturn(null)
        whenever(parseService.parseResume(any())).thenReturn("parsed content")
        whenever(storageService.uploadResume(any(), eq(accountId))).thenReturn("resumes/key")
        whenever(storageService.saveResume(any())).thenReturn(saved)
        whenever(analysisPublisher.publishResumeAnalyze(eq(saved.id), eq(saved.accountId), any()))
            .thenThrow(RuntimeException("redis unavailable"))

        assertThrows(AppException::class.java) {
            service.uploadAndAnalyze(
                ResumeUploadCmd(
                    file = MockMultipartFile(
                        "file",
                        "resume.pdf",
                        "application/pdf",
                        "pdf".toByteArray(),
                    ),
                    accountId = accountId,
                ),
            )
        }

        verify(storageService).markAnalyzeFailed(eq(saved.id), any())
    }

    private fun resume(
        accountId: UUID,
        filename: String,
        status: ResumeStatus,
    ): Resume {
        val now = OffsetDateTime.now()
        return Resume(
            id = UUID.randomUUID(),
            accountId = accountId,
            filename = filename,
            content = "content",
            fileHash = "hash",
            size = 123,
            storageKey = "resumes/key",
            status = status,
            error = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}
