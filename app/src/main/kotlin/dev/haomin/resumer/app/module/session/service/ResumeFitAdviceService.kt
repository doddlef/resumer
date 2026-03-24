package dev.haomin.resumer.app.module.session.service

import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.service.dto.ResumeFitAdviceGenerateCmd

interface ResumeFitAdviceService {
    fun generate(cmd: ResumeFitAdviceGenerateCmd): SessionAdvice
}
