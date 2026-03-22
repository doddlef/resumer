package dev.haomin.resumer.app.auth.prop

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "resumer.auth.refresh")
data class RefreshTokenProperties(
    val secret: String,
    val idleLifetime: Duration = Duration.ofDays(7),
    val maxLifetime: Duration = Duration.ofDays(120),
    val graceTime: Duration = Duration.ofSeconds(5),
)