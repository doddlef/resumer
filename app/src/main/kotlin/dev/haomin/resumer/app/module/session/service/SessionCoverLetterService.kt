package dev.haomin.resumer.app.module.session.service

import dev.haomin.resumer.app.module.session.domain.SessionCoverLetter
import dev.haomin.resumer.app.module.session.service.dto.SessionCoverLetterGenerateCmd
import dev.haomin.resumer.app.module.session.service.dto.SessionCoverLetterGetCmd

interface SessionCoverLetterService {
    fun generate(cmd: SessionCoverLetterGenerateCmd): SessionCoverLetter

    fun get(cmd: SessionCoverLetterGetCmd): SessionCoverLetter
}
