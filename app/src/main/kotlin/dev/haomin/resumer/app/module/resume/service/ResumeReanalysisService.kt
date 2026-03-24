package dev.haomin.resumer.app.module.resume.service

import dev.haomin.resumer.app.module.resume.service.dto.ResumeReanalyzeCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeReanalyzeResult

interface ResumeReanalysisService {
    fun reanalyze(cmd: ResumeReanalyzeCmd): ResumeReanalyzeResult
}
