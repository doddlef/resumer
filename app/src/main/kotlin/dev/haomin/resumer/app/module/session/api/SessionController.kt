package dev.haomin.resumer.app.module.session.api

import dev.haomin.resumer.app.auth.principal.PrincipalProvider
import dev.haomin.resumer.app.common.response.ApiResponse
import dev.haomin.resumer.app.module.session.api.dto.CreateSessionRequest
import dev.haomin.resumer.app.module.session.service.ApplicationSessionService
import dev.haomin.resumer.app.module.session.service.SessionAdviceReadService
import dev.haomin.resumer.app.module.session.service.SessionAdviceTaskService
import dev.haomin.resumer.app.module.session.service.SessionCoverLetterService
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionCreateCmd
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionDetailCmd
import dev.haomin.resumer.app.module.session.service.dto.ApplicationSessionListCmd
import dev.haomin.resumer.app.module.session.service.dto.SessionAdviceReadCmd
import dev.haomin.resumer.app.module.session.service.dto.SessionCoverLetterGenerateCmd
import dev.haomin.resumer.app.module.session.service.dto.SessionCoverLetterGetCmd
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/sessions")
class SessionController(
    private val principalProvider: PrincipalProvider,
    private val applicationSessionService: ApplicationSessionService,
    private val sessionAdviceTaskService: SessionAdviceTaskService,
    private val sessionAdviceReadService: SessionAdviceReadService,
    private val sessionCoverLetterService: SessionCoverLetterService,
) {

    @PostMapping
    fun create(
        @RequestBody request: CreateSessionRequest,
    ): ResponseEntity<ApiResponse> {
        val result = applicationSessionService.create(
            ApplicationSessionCreateCmd(
                accountId = principalProvider.requireCurrentId(),
                resumeId = request.resumeId,
                company = request.company,
                position = request.position,
                jobDescription = request.jobDescription,
            ),
        )
        val body = ApiResponse.success("session created")
            .with("session", result)
            .build()
        return ResponseEntity.ok(body)
    }

    @GetMapping
    fun list(): ResponseEntity<ApiResponse> {
        val result = applicationSessionService.list(
            ApplicationSessionListCmd(
                accountId = principalProvider.requireCurrentId(),
            ),
        )
        val body = ApiResponse.success("sessions listed")
            .with("sessions", result.sessions)
            .build()
        return ResponseEntity.ok(body)
    }

    @GetMapping("/{sessionId}")
    fun detail(
        @PathVariable sessionId: UUID,
    ): ResponseEntity<ApiResponse> {
        val result = applicationSessionService.detail(
            ApplicationSessionDetailCmd(
                sessionId = sessionId,
                accountId = principalProvider.requireCurrentId(),
            ),
        )
        val body = ApiResponse.success("session detail fetched")
            .with("session", result)
            .build()
        return ResponseEntity.ok(body)
    }

    @PostMapping("/{sessionId}/advice/position")
    fun queuePositionAdvice(
        @PathVariable sessionId: UUID,
    ): ResponseEntity<ApiResponse> {
        val result = sessionAdviceTaskService.queuePositionAdvice(
            sessionId = sessionId,
            accountId = principalProvider.requireCurrentId(),
        )
        val body = ApiResponse.success("position advice queued")
            .with("task", result)
            .build()
        return ResponseEntity.ok(body)
    }

    @PostMapping("/{sessionId}/advice/resume-fit")
    fun queueResumeFitAdvice(
        @PathVariable sessionId: UUID,
    ): ResponseEntity<ApiResponse> {
        val result = sessionAdviceTaskService.queueResumeFit(
            sessionId = sessionId,
            accountId = principalProvider.requireCurrentId(),
        )
        val body = ApiResponse.success("resume fit advice queued")
            .with("task", result)
            .build()
        return ResponseEntity.ok(body)
    }

    @GetMapping("/{sessionId}/advice")
    fun latestAdvice(
        @PathVariable sessionId: UUID,
    ): ResponseEntity<ApiResponse> {
        val result = sessionAdviceReadService.latest(
            SessionAdviceReadCmd(
                sessionId = sessionId,
                accountId = principalProvider.requireCurrentId(),
            ),
        )
        val body = ApiResponse.success("session advice fetched")
            .with("advice", result)
            .build()
        return ResponseEntity.ok(body)
    }

    @PostMapping("/{sessionId}/cover-letters")
    fun generateCoverLetter(
        @PathVariable sessionId: UUID,
    ): ResponseEntity<ApiResponse> {
        val result = sessionCoverLetterService.generate(
            SessionCoverLetterGenerateCmd(
                sessionId = sessionId,
                accountId = principalProvider.requireCurrentId(),
                traceId = "cover-letter-$sessionId",
            ),
        )
        val body = ApiResponse.success("cover letter generated")
            .with("cover_letter", result)
            .build()
        return ResponseEntity.ok(body)
    }

    @GetMapping("/{sessionId}/cover-letters")
    fun getCoverLetter(
        @PathVariable sessionId: UUID,
        @RequestParam(required = false) version: Int?,
    ): ResponseEntity<ApiResponse> {
        val result = sessionCoverLetterService.get(
            SessionCoverLetterGetCmd(
                sessionId = sessionId,
                accountId = principalProvider.requireCurrentId(),
                version = version,
            ),
        )
        val body = ApiResponse.success("cover letter fetched")
            .with("cover_letter", result)
            .build()
        return ResponseEntity.ok(body)
    }
}
