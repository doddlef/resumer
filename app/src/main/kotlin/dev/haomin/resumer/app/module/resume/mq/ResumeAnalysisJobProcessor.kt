package dev.haomin.resumer.app.module.resume.mq

import dev.haomin.resumer.app.module.resume.mq.model.ResumeAnalyzePayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface ResumeAnalysisJobProcessor {
    fun process(payload: ResumeAnalyzePayload)
}

@Service
class PlaceholderResumeAnalysisJobProcessor : ResumeAnalysisJobProcessor {
    private val logger = LoggerFactory.getLogger(PlaceholderResumeAnalysisJobProcessor::class.java)

    override fun process(payload: ResumeAnalyzePayload) {
        // Placeholder: wire real resume analysis pipeline here.
        logger.info(
            "[placeholder] resume analysis requested: resumeId={}, accountId={}",
            payload.resumeId,
            payload.accountId,
        )
    }
}
