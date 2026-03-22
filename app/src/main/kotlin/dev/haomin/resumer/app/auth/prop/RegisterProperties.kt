package dev.haomin.resumer.app.auth.prop

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "resumer.auth.register")
data class RegisterProperties(
    val codeHashSecret: String,
    val codeLength: Int = 6,
    val maxVerifyTries: Int = 5,
    val attemptLifetime: Duration = Duration.ofMinutes(15),
    val resendCooldown: Duration = Duration.ofSeconds(60),
)
