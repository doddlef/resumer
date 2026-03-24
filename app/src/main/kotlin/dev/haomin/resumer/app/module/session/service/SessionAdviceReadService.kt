package dev.haomin.resumer.app.module.session.service

import dev.haomin.resumer.app.module.session.domain.SessionAdvice
import dev.haomin.resumer.app.module.session.service.dto.SessionAdviceReadCmd

interface SessionAdviceReadService {
    fun latest(cmd: SessionAdviceReadCmd): SessionAdvice
}
