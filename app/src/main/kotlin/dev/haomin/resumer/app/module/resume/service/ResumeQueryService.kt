package dev.haomin.resumer.app.module.resume.service

import dev.haomin.resumer.app.module.resume.service.dto.ResumeDetailCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeDetailResult
import dev.haomin.resumer.app.module.resume.service.dto.ResumeListCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeListResult
import dev.haomin.resumer.app.module.resume.service.dto.ResumeStatusPollCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeStatusPollResult

interface ResumeQueryService {
    fun list(cmd: ResumeListCmd): ResumeListResult

    fun detail(cmd: ResumeDetailCmd): ResumeDetailResult

    fun pollStatus(cmd: ResumeStatusPollCmd): ResumeStatusPollResult
}
