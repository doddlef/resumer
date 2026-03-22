package dev.haomin.resumer.app.infra.file.prop

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resumer.file.parser")
data class ParserProperties(
    val maxLength: Int = 5 * 1024 * 1024,
)
