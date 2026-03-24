package dev.haomin.resumer.app.module.session.service

import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionCreateCmd
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionCreateResult
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionDetailCmd
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionDetailResult
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionListCmd
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionListResult

interface ApplicationSessionService {
    fun create(cmd: ApplicationSessionCreateCmd): ApplicationSessionCreateResult

    fun list(cmd: ApplicationSessionListCmd): ApplicationSessionListResult

    fun detail(cmd: ApplicationSessionDetailCmd): ApplicationSessionDetailResult
}
