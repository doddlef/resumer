package dev.haomin.resumer.app.module.session.domain

enum class TaskStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    companion object {
        fun fromString(value: String): TaskStatus =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid TaskStatus: $value")
    }
}
