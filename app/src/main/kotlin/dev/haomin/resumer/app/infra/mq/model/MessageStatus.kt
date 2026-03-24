package dev.haomin.resumer.app.infra.mq.model

/**
 * Represents the various statuses that a message can have during its lifecycle in a message
 * queue-based system.
 *
 * This enum is used to track and categorize the current processing state of a message.
 *
 * - `PENDING`: Indicates that the message is awaiting processing.
 * - `PROCESSING`: The message is actively being processed.
 * - `RETRYING`: The message is being retried after a failure.
 * - `COMPLETED`: The message was successfully processed.
 * - `FAILED`: The message processing failed and will not be retried further.
 * - `DEAD_LETTER`: The message has been moved to a dead letter queue due to persistent failures.
 */
enum class MessageStatus {
    PENDING,
    PROCESSING,
    RETRYING,
    COMPLETED,
    FAILED,
    DEAD_LETTER,
}
