package dev.haomin.resumer.app.infra.storage.prop

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "resumer.storage")
data class StorageProperties(
    val engine: String = "local",
    val local: Local = Local(),
) {
    data class Local(
        val baseDir: String = "./.local-storage",
        val publicBaseUrl: String? = null,
        val tempUrlSecret: String = "local_temp_url_secret_change_me_12345678901234567890",
        val defaultTtl: Duration = Duration.ofMinutes(5),
    )
}
