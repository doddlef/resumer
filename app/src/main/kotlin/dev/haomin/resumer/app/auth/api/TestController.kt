package dev.haomin.resumer.app.auth.api

import dev.haomin.resumer.app.auth.principal.PrincipalProvider
import dev.haomin.resumer.app.common.response.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import java.time.Instant

@RestController
@RequestMapping("/api")
class TestController(
    private val provider: PrincipalProvider,
) {

    @GetMapping("/public/ping")
    fun ping(): ResponseEntity<ApiResponse> {
        val currentPrincipal = provider.current()
        return ApiResponse.success("pong")
            .withIf("account_id", currentPrincipal?.id, currentPrincipal != null)
            .with("at", Instant.now().toEpochMilli())
            .build()
            .let { ResponseEntity.ok(it) }
    }
}
