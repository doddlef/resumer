package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.module.resume.domain.Resume
import dev.haomin.resumer.app.module.resume.repo.ResumeAnalysisRepo
import dev.haomin.resumer.app.module.resume.repo.ResumeRepo
import dev.haomin.resumer.app.module.resume.service.ResumeQueryService
import dev.haomin.resumer.app.module.resume.service.dto.ResumeDetailCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeDetailResult
import dev.haomin.resumer.app.module.resume.service.dto.ResumeListCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeListItem
import dev.haomin.resumer.app.module.resume.service.dto.ResumeListResult
import dev.haomin.resumer.app.module.resume.service.dto.ResumeStatusPollCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeStatusPollResult
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ResumeQueryServiceImpl(
    private val resumeRepo: ResumeRepo,
    private val resumeAnalysisRepo: ResumeAnalysisRepo,
) : ResumeQueryService {

    override fun list(cmd: ResumeListCmd): ResumeListResult {
        val resumes = resumeRepo.selectByAccountId(cmd.accountId)
        val latestAnalysisByResumeId = resumeAnalysisRepo.selectLatestByResumeIds(resumes.map { it.id })
        return ResumeListResult(
            resumes = resumes.map { resume ->
                ResumeListItem(
                    id = resume.id,
                    name = resume.filename,
                    status = resume.status,
                    score = latestAnalysisByResumeId[resume.id]?.score,
                    createdAt = resume.createdAt,
                )
            },
        )
    }

    override fun detail(cmd: ResumeDetailCmd): ResumeDetailResult {
        val resume = requireOwnedResume(cmd.resumeId, cmd.accountId)
        val latestAnalysis = resumeAnalysisRepo.selectLatestByResumeId(resume.id)
        return ResumeDetailResult(
            id = resume.id,
            name = resume.filename,
            status = resume.status,
            score = latestAnalysis?.score,
            summary = latestAnalysis?.summary,
            strengths = latestAnalysis?.strengths ?: emptyList(),
            suggestions = latestAnalysis?.suggestions ?: emptyList(),
            error = resume.error,
            createdAt = resume.createdAt,
            updatedAt = resume.updatedAt,
        )
    }

    override fun pollStatus(cmd: ResumeStatusPollCmd): ResumeStatusPollResult {
        val resume = requireOwnedResume(cmd.resumeId, cmd.accountId)
        val latestAnalysis = resumeAnalysisRepo.selectLatestByResumeId(resume.id)
        return ResumeStatusPollResult(
            resumeId = resume.id,
            status = resume.status,
            score = latestAnalysis?.score,
            error = resume.error,
            updatedAt = resume.updatedAt,
        )
    }

    private fun requireOwnedResume(resumeId: UUID, accountId: UUID): Resume {
        val resume = resumeRepo.selectById(resumeId)
            ?: throw NotFoundException("resume not found")
        if (resume.accountId != accountId) {
            throw NotFoundException("resume not found")
        }
        return resume
    }
}
