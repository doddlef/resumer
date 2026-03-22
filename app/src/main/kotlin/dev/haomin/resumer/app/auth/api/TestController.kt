package dev.haomin.resumer.app.auth.api

import dev.haomin.resumer.app.auth.principal.PrincipalProvider
import dev.haomin.resumer.app.common.response.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping("/api")
class TestController(
    private val provider: PrincipalProvider,
) {

    @GetMapping("/public/ping")
    fun ping(): ResponseEntity<ApiResponse> {
        val currentPrincipal = provider.current()
        return ApiResponse.success("pong")
            .build()
            .let { ResponseEntity.ok(it) }
    }
}
