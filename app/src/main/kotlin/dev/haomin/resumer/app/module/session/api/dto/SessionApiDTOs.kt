package dev.haomin.resumer.app.module.session.api.dto

import java.util.UUID

data class CreateSessionRequest(
    val resumeId: UUID? = null,
    val company: String,
    val position: String,
    val jobDescription: String,
)
