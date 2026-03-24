package dev.haomin.resumer.app.module.resume.mq

import dev.haomin.resumer.app.module.resume.mq.model.ResumeAnalyzePayload
import dev.haomin.resumer.app.module.resume.service.ResumeAnalysisService
import dev.haomin.resumer.app.module.resume.service.dto.ResumeAnalyzeCmd
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface ResumeAnalysisJobProcessor {
    fun process(payload: ResumeAnalyzePayload)
}

@Service
class ResumeAnalysisJobProcessorImpl(
    private val resumeAnalysisService: ResumeAnalysisService,
) : ResumeAnalysisJobProcessor {
    private val logger = LoggerFactory.getLogger(ResumeAnalysisJobProcessorImpl::class.java)

    override fun process(payload: ResumeAnalyzePayload) {
        val cmd = ResumeAnalyzeCmd(
            resumeId = payload.resumeId,
            accountId = payload.accountId,
            traceId = "resume-analysis-${payload.resumeId}",
        )
        resumeAnalysisService.analyze(cmd)
        logger.info(
            "resume analysis finished: resumeId={}, accountId={}",
            payload.resumeId,
            payload.accountId,
        )
    }
}
