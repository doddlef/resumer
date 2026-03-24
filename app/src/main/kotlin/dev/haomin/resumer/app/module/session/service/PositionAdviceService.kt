package dev.haomin.resumer.app.module.session.service

import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.service.dto.PositionAdviceGenerateCmd

interface PositionAdviceService {
    fun generate(cmd: PositionAdviceGenerateCmd): SessionAdvice
}
