package dev.haomin.resumer.app.infra.mq.model

/**
 * Represents a message that has been moved to the dead letter queue due to persistent failures.
 *
 * This data class is used to encapsulate details of a failed message, including tracking information
 * and the associated error details. It provides insights into the original stream, message, failure
 * reason, and retry attempt information. Such messages are typically not retried further and require
 * manual intervention or specialized handling.
 *
 * @property originalStream The name of the stream where the message originated from.
 * @property originalMessageId The unique identifier of the original message that encountered persistent failures.
 * @property wrapperJson The serialized JSON representation of the original message wrapper.
 * @property errorType The type or category of the error that led to the message being dead-lettered.
 * @property errorMessage A descriptive error message detailing the cause of failure.
 * @property attempt The number of attempts made to process the message before being moved to the dead letter queue.
 * @property failedAtEpochMs The timestamp (epoch time in milliseconds) when the message was marked as dead-lettered.
 */
data class DeadLetterMessage(
    val originalStream: String,
    val originalMessageId: String,
    val wrapperJson: String,
    val errorType: String,
    val errorMessage: String,
    val attempt: Int,
    val failedAtEpochMs: Long,
)
