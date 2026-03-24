package dev.haomin.resumer.app.module.resume.service

import dev.haomin.resumer.app.module.resume.domain.ResumeAnalysis
import dev.haomin.resumer.app.module.resume.service.dto.ResumeAnalyzeCmd

interface ResumeAnalysisService {

    /**
     * Analyze the resume and return the analysis result.
     *
     * @param cmd the command containing the resume ID and account ID
     * @return the analysis result of the resume
     */
    fun analyze(cmd: ResumeAnalyzeCmd): ResumeAnalysis
}
