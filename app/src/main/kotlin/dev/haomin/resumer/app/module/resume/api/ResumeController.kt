package dev.haomin.resumer.app.module.resume.api

import dev.haomin.resumer.app.auth.principal.PrincipalProvider
import dev.haomin.resumer.app.common.response.ApiResponse
import dev.haomin.resumer.app.module.resume.service.ResumeQueryService
import dev.haomin.resumer.app.module.resume.service.ResumeReanalysisService
import dev.haomin.resumer.app.module.resume.service.ResumeUploadService
import dev.haomin.resumer.app.module.resume.service.dto.ResumeDetailCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeListCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeReanalyzeCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeStatusPollCmd
import dev.haomin.resumer.app.module.resume.service.dto.ResumeUploadCmd
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/resumes")
class ResumeController(
    private val principalProvider: PrincipalProvider,
    private val resumeUploadService: ResumeUploadService,
    private val resumeReanalysisService: ResumeReanalysisService,
    private val resumeQueryService: ResumeQueryService,
) {

    @PostMapping("/upload-and-analyze")
    fun uploadAndAnalyze(
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<ApiResponse> {
        val result = resumeUploadService.uploadAndAnalyze(
            ResumeUploadCmd(
                file = file,
                accountId = principalProvider.requireCurrentId(),
            )
        )
        val body = ApiResponse.success("resume uploaded")
            .with("resume", result)
            .build()
        return ResponseEntity.ok(body)
    }

    @PostMapping("/{resumeId}/reanalyze")
    fun reanalyze(
        @PathVariable resumeId: UUID,
    ): ResponseEntity<ApiResponse> {
        val result = resumeReanalysisService.reanalyze(
            ResumeReanalyzeCmd(
                resumeId = resumeId,
                accountId = principalProvider.requireCurrentId(),
            ),
        )
        val body = ApiResponse.success("resume reanalysis queued")
            .with("resume", result)
            .build()
        return ResponseEntity.ok(body)
    }

    @GetMapping
    fun listResumes(): ResponseEntity<ApiResponse> {
        val result = resumeQueryService.list(
            ResumeListCmd(
                accountId = principalProvider.requireCurrentId(),
            ),
        )
        val body = ApiResponse.success("resumes listed")
            .with("resumes", result.resumes)
            .build()
        return ResponseEntity.ok(body)
    }

    @GetMapping("/{resumeId}")
    fun getResumeDetail(
        @PathVariable resumeId: UUID,
    ): ResponseEntity<ApiResponse> {
        val result = resumeQueryService.detail(
            ResumeDetailCmd(
                resumeId = resumeId,
                accountId = principalProvider.requireCurrentId(),
            ),
        )
        val body = ApiResponse.success("resume detail fetched")
            .with("resume", result)
            .build()
        return ResponseEntity.ok(body)
    }

    @GetMapping("/{resumeId}/status")
    fun pollResumeStatus(
        @PathVariable resumeId: UUID,
    ): ResponseEntity<ApiResponse> {
        val result = resumeQueryService.pollStatus(
            ResumeStatusPollCmd(
                resumeId = resumeId,
                accountId = principalProvider.requireCurrentId(),
            ),
        )
        val body = ApiResponse.success("resume status fetched")
            .with("resume", result)
            .build()
        return ResponseEntity.ok(body)
    }
}
