package dev.haomin.resumer.app.auth.api

import dev.haomin.resumer.app.auth.REFRESH_TOKEN_COOKIE
import dev.haomin.resumer.app.auth.prop.RefreshTokenProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class RefreshCookieManager(
    private val refreshTokenProperties: RefreshTokenProperties,
) {
    fun build(refreshToken: String): ResponseCookie =
        ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
            .httpOnly(true)
            .secure(true)
            .path("/api/auth")
            .sameSite("Lax")
            .maxAge(refreshTokenProperties.maxLifetime)
            .build()

    fun clear(): ResponseCookie =
        ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(true)
            .path("/api/auth")
            .sameSite("Lax")
            .maxAge(0)
            .build()
}
