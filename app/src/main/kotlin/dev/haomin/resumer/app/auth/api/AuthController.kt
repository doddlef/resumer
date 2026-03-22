package dev.haomin.resumer.app.auth.api

import dev.haomin.resumer.app.auth.REFRESH_TOKEN_COOKIE
import dev.haomin.resumer.app.auth.api.dto.AuthLoginRequest
import dev.haomin.resumer.app.auth.service.AuthService
import dev.haomin.resumer.app.auth.service.TokenService
import dev.haomin.resumer.app.auth.service.dto.EmailPwdLoginCmd
import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.response.ApiResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
    private val cookieManager: RefreshCookieManager,
) {

    @PostMapping
    fun login(@RequestBody request: AuthLoginRequest): ResponseEntity<ApiResponse> {
        val principal = authService.emailPasswordLogin(
            EmailPwdLoginCmd(
                email = request.email,
                password = request.password,
            ),
        )
        val issued = tokenService.login(principal.id)
        val refreshCookie = cookieManager.build(issued.tokenPair.refresh.token)
        val body = ApiResponse.success("login successful")
            .with("account", issued.account)
            .with("token_pair", issued.tokenPair)
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(body)
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) refreshToken: String?,
    ): ResponseEntity<ApiResponse> {
        val token = requireRefreshToken(refreshToken)
        val issued = tokenService.refresh(token)
        val refreshCookie = cookieManager.build(issued.tokenPair.refresh.token)
        val body = ApiResponse.success("token refreshed")
            .with("account", issued.account)
            .with("token_pair", issued.tokenPair)
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(body)
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) refreshToken: String?,
    ): ResponseEntity<ApiResponse> {
        val revoked = refreshToken
            ?.takeIf { it.isNotBlank() }
            ?.let { tokenService.logout(it) }
            ?: false
        val clearCookie = cookieManager.clear()
        val body = ApiResponse.success("logout successful")
            .with("revoked", revoked)
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
            .body(body)
    }

    private fun requireRefreshToken(refreshToken: String?): String =
        refreshToken
            ?.takeIf { it.isNotBlank() }
            ?: throw InvalidParamException("refresh token is required")
}
