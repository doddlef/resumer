package dev.haomin.resumer.app.module.resume.api

import dev.haomin.resumer.app.auth.principal.PrincipalProvider
import dev.haomin.resumer.app.common.response.ApiResponse
import dev.haomin.resumer.app.module.resume.service.ResumeUploadService
import dev.haomin.resumer.app.module.resume.service.dto.ResumeUploadCmd
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/resumes")
class ResumeController(
    private val principalProvider: PrincipalProvider,
    private val resumeUploadService: ResumeUploadService,
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
}
