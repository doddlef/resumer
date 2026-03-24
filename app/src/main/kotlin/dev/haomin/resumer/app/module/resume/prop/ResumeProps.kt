package dev.haomin.resumer.app.module.resume.prop

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resumer.resume")
data class ResumeProps(
    val maxFileSizeBytes: Long = 10L * 1024 * 1024,
    val supportedContentTypes: Set<String> = setOf(
        "application/pdf",
        "text/plain",
    ),
)
