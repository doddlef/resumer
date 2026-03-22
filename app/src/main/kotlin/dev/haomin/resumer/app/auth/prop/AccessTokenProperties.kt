package dev.haomin.resumer.app.auth.prop

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "resumer.auth.access")
data class AccessTokenProperties(
    val secret: String,
    val lifetime: Duration = Duration.ofMinutes(30),
)
