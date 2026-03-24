package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.domain.ResumeAnalysis
import dev.haomin.resumer.app.module.resume.domain.ResumeStatus
import dev.haomin.resumer.app.module.resume.domain.ResumeSuggestion
import dev.haomin.resumer.app.module.resume.repo.ResumeAnalysisRepo
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.resume.service.dto.ResumeDetailCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeListCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeStatusPollCmd
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
class ResumeQueryServiceImplTest {

    @Mock
    private lateinit var resumeRepo: ResumeRepo

    @Mock
    private lateinit var resumeAnalysisRepo: ResumeAnalysisRepo

    @Test
    fun `list should return name status score for account resumes`() {
        val service = ResumeQueryServiceImpl(resumeRepo, resumeAnalysisRepo)
        val accountId = UUID.randomUUID()
        val resumeA = resume(accountId = accountId, filename = "a.pdf", status = ResumeStatus.COMPLETED)
        val resumeB = resume(accountId = accountId, filename = "b.pdf", status = ResumeStatus.PENDING)
        whenever(resumeRepo.selectByAccountId(eq(accountId))).thenReturn(listOf(resumeA, resumeB))
        whenever(resumeAnalysisRepo.selectLatestByResumeIds(any())).thenReturn(
            mapOf(resumeA.id to analysis(resumeA.id, 88)),
        )

        val result = service.list(ResumeListCmd(accountId = accountId))

        assertEquals(2, result.resumes.size)
        assertEquals(resumeA.id, result.resumes[0].id)
        assertEquals("a.pdf", result.resumes[0].name)
        assertEquals(88, result.resumes[0].score)
        assertEquals(null, result.resumes[1].score)
    }

    @Test
    fun `detail should return latest analysis when present`() {
        val service = ResumeQueryServiceImpl(resumeRepo, resumeAnalysisRepo)
        val resume = resume()
        val analysis = analysis(resume.id, 91)
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)
        whenever(resumeAnalysisRepo.selectLatestByResumeId(eq(resume.id))).thenReturn(analysis)

        val result = service.detail(
            ResumeDetailCmd(
                resumeId = resume.id,
                accountId = resume.accountId,
            ),
        )

        assertEquals(resume.id, result.id)
        assertEquals(resume.filename, result.name)
        assertEquals(91, result.score)
        assertEquals(analysis.summary, result.summary)
        assertEquals(analysis.strengths, result.strengths)
        assertEquals(analysis.suggestions, result.suggestions)
    }

    @Test
    fun `poll status should return status and score`() {
        val service = ResumeQueryServiceImpl(resumeRepo, resumeAnalysisRepo)
        val resume = resume(status = ResumeStatus.ANALYZING)
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)
        whenever(resumeAnalysisRepo.selectLatestByResumeId(eq(resume.id))).thenReturn(analysis(resume.id, 76))

        val result = service.pollStatus(
            ResumeStatusPollCmd(
                resumeId = resume.id,
                accountId = resume.accountId,
            ),
        )

        assertEquals(resume.id, result.resumeId)
        assertEquals(ResumeStatus.ANALYZING, result.status)
        assertEquals(76, result.score)
    }

    @Test
    fun `detail should throw not found when account mismatched`() {
        val service = ResumeQueryServiceImpl(resumeRepo, resumeAnalysisRepo)
        val resume = resume()
        whenever(resumeRepo.selectById(eq(resume.id))).thenReturn(resume)

        assertThrows(NotFoundException::class.java) {
            service.detail(
                ResumeDetailCmd(
                    resumeId = resume.id,
                    accountId = UUID.randomUUID(),
                ),
            )
        }

        verify(resumeAnalysisRepo, never()).selectLatestByResumeId(any())
    }

    private fun resume(
        accountId: UUID = UUID.randomUUID(),
        filename: String = "resume.pdf",
        status: ResumeStatus = ResumeStatus.COMPLETED,
    ): Resume {
        val now = OffsetDateTime.now()
        return Resume(
            id = UUID.randomUUID(),
            accountId = accountId,
            filename = filename,
            content = "resume content",
            fileHash = "hash",
            size = 123L,
            storageKey = "resumes/key",
            status = status,
            error = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun analysis(resumeId: UUID, score: Int): ResumeAnalysis {
        val now = OffsetDateTime.now()
        return ResumeAnalysis(
            id = UUID.randomUUID(),
            resumeId = resumeId,
            score = score,
            summary = "Summary",
            strengths = listOf("Strength 1"),
            suggestions = listOf(
                ResumeSuggestion(
                    category = "content",
                    priority = "high",
                    issue = "Issue",
                    recommendation = "Recommendation",
                ),
            ),
            createdAt = now,
        )
    }
}
