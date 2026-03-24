package dev.haomin.resumer.app.infra.mq.model

/**
 * Represents a wrapper for messages with metadata and status tracking.
 *
 * This data class is used to encapsulate a message with its associated metadata, status, and
 * tracking information for message processing in a message queue-based system.
 *
 * @param T The type of the payload contained within the message wrapper.
 * @property id The unique identifier for the message.
 * @property type A string indicating the type/category of the message.
 * @property payload The payload of the message, which can be of any generic type.
 * @property attempt The number of attempts made to process the message. Defaults to 0.
 * @property status The current status of the message. Default is `MessageStatus.PENDING`.
 * @property traceId A unique trace identifier for tracking the message across systems.
 * @property createdAtEpochMs The timestamp in milliseconds (epoch) when the message was created.
 * @property updatedAtEpochMs The timestamp in milliseconds (epoch) when the message was last updated.
 */
data class MessageWrapper<T>(
    val id: String,
    val type: String,
    val payload: T,
    val attempt: Int = 0,
    val status: MessageStatus = MessageStatus.PENDING,
    val traceId: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
