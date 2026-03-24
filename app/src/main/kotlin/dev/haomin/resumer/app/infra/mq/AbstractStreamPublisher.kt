package dev.haomin.resumer.app.infra.mq

import dev.haomin.resumer.app.framework.redis.RedisClient
import dev.haomin.resumer.app.infra.mq.model.MessageWrapper
import org.slf4j.Logger

/**
 * Abstract base class for publishing messages to a Redis Stream.
 *
 * This class provides a mechanism to publish messages to a Redis Stream and handle
 * post-publication events, such as successful publishing or publish failures.
 *
 * @param T The type of the message payload being serialized and published.
 * @property redisClient The Redis client used for interacting with the Redis Stream.
 */
abstract class AbstractStreamPublisher<T>(
    private val redisClient: RedisClient,
) {
    protected abstract val streamKey: String
    protected abstract val streamMaxLen: Long?
    protected abstract val serde: StreamSerde<T>

    private companion object {
        val logger: Logger = org.slf4j.LoggerFactory.getLogger(AbstractStreamPublisher::class.java)
    }

    /**
     * Publishes a message to the configured Redis stream.
     *
     * This method serializes the provided `MessageWrapper` object, adds it to the Redis stream,
     * and triggers post-publication callbacks for success or failure scenarios. If an error occurs
     * during the publish process, it will be logged and rethrown.
     *
     * @param wrapper The `MessageWrapper` object containing the message to be published to the Redis stream.
     * @return The unique message ID assigned by Redis upon successful publishing.
     * @throws Exception If publishing to the Redis stream fails, the exception will be rethrown after logging.
     */
    fun publish(wrapper: MessageWrapper<T>): String {
        return try {
            val messageId = redisClient.streamAdd(streamKey, serde.toFields(wrapper), streamMaxLen)
            onPublished(wrapper, messageId)
            messageId
        } catch (e: Exception) {
            logger.error("Publish message failed: stream={}, id={}, type={}, err={}", streamKey, wrapper.id, wrapper.type, e.message, e)
            onPublishFailed(wrapper, e)
            throw e
        }
    }

    /**
     * Callback method invoked after a message has been successfully published.
     *
     * This method provides an extension point for handling logic that should occur
     * once a message is published to the Redis stream. It logs the publication event
     * along with the associated stream key, message ID, and Redis-generated message ID.
     *
     * @param wrapper The `MessageWrapper` object containing the details of the published message.
     * @param messageId The unique message ID assigned by Redis upon successful publication.
     */
    protected open fun onPublished(wrapper: MessageWrapper<T>, messageId: String) {
        logger.info("Message published: stream={}, id={}, messageId={}", streamKey, wrapper.id, messageId)
    }

    /**
     * Callback method invoked when a message fails to publish to the Redis stream.
     *
     * This method serves as an extension point for handling logic that should be executed
     * in the event of a publication failure. Subclasses can override this method to
     * perform custom error handling such as logging, retrying, or updating metrics.
     *
     * @param wrapper The `MessageWrapper` object containing the details of the message
     *                that failed to publish. This includes information such as the
     *                message ID, payload, and status.
     * @param error   The `Throwable` instance representing the error that occurred
     *                during the publish attempt.
     */
    protected open fun onPublishFailed(wrapper: MessageWrapper<T>, error: Throwable) {
        logger.error(
            "Publish message failed: stream={}, id={}, type={}, err={}",
            streamKey,
            wrapper.id,
            wrapper.type,
            error.message,
            error
        )
    }
}
