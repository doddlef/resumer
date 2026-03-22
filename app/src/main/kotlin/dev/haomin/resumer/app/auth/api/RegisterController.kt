package dev.haomin.resumer.app.auth.api

import dev.haomin.resumer.app.auth.api.dto.RegisterConfirmRequest
import dev.haomin.resumer.app.auth.api.dto.RegisterResendRequest
import dev.haomin.resumer.app.auth.api.dto.RegisterStartRequest
import dev.haomin.resumer.app.auth.api.dto.RegisterVerifyRequest
import dev.haomin.resumer.app.auth.service.RegisterService
import dev.haomin.resumer.app.auth.service.TokenService
import dev.haomin.resumer.app.auth.service.dto.CompleteRegistrationCmd
import dev.haomin.resumer.app.auth.service.dto.ResendVerificationCmd
import dev.haomin.resumer.app.auth.service.dto.SendVerificationCmd
import dev.haomin.resumer.app.auth.service.dto.VerifyCodeCmd
import dev.haomin.resumer.app.common.response.ApiResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/register")
class RegisterController(
    private val registerService: RegisterService,
    private val tokenService: TokenService,
    private val cookieManager: RefreshCookieManager,
) {
    @PostMapping("/start")
    fun start(@RequestBody request: RegisterStartRequest): ResponseEntity<ApiResponse> {
        val result = registerService.sendVerificationEmail(
            SendVerificationCmd(email = request.email),
        )
        val body = ApiResponse.success("verification code sent")
            .with("attempt_id", result.attemptId)
            .with("expires_in_seconds", result.expiresInSeconds)
            .build()
        return ResponseEntity.ok(body)
    }

    @PostMapping("/verify")
    fun verify(@RequestBody request: RegisterVerifyRequest): ResponseEntity<ApiResponse> {
        val result = registerService.verifyCode(
            VerifyCodeCmd(
                attemptId = request.attemptId,
                code = request.code,
            ),
        )
        val body = ApiResponse.success("verification complete")
            .with("verified", result.verified)
            .with("verified_at", result.verifiedAt)
            .build()
        return ResponseEntity.ok(body)
    }

    @PostMapping("/")
    fun confirm(@RequestBody request: RegisterConfirmRequest): ResponseEntity<ApiResponse> {
        val result = registerService.completeRegistration(
            CompleteRegistrationCmd(
                attemptId = request.attemptId,
                nickname = request.nickname,
                password = request.password,
            ),
        )
        val issued = tokenService.login(result.accountId)
        val refreshCookie = cookieManager.build(issued.tokenPair.refresh.token)
        val body = ApiResponse.success("registration complete")
            .with("account", issued.account)
            .with("token_pair", issued.tokenPair)
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(body)
    }

    @PostMapping("/resend")
    fun resend(@RequestBody request: RegisterResendRequest): ResponseEntity<ApiResponse> {
        val result = registerService.resendVerification(
            ResendVerificationCmd(attemptId = request.attemptId),
        )
        val body = ApiResponse.success("verification code resent")
            .with("attempt_id", result.attemptId)
            .with("expires_in_seconds", result.expiresInSeconds)
            .build()
        return ResponseEntity.ok(body)
    }
}
