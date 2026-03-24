package dev.haomin.resumer.app.module.resume.repo

import dev.haomin.resumer.app.module.resume.domain.ResumeAnalysis
import dev.haomin.resumer.app.module.resume.repo.query.ResumeAnalysisInsertQuery
import java.util.UUID

interface ResumeAnalysisRepo {
    fun selectLatestByResumeId(resumeId: UUID): ResumeAnalysis?

    fun insertAndReturn(query: ResumeAnalysisInsertQuery): ResumeAnalysis
}
