package dev.haomin.resumer.app.infra.mq

import dev.haomin.resumer.app.framework.redis.RedisClient
import dev.haomin.resumer.app.infra.mq.model.DeadLetterMessage
import dev.haomin.resumer.app.infra.mq.model.MessageStatus
import dev.haomin.resumer.app.infra.mq.model.MessageWrapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.Logger
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Abstract base class for implementing a Redis Stream consumer.
 *
 * This class provides the necessary infrastructure for handling a Redis Stream and processing
 * its messages in a consumer group context. It includes features such as message acknowledgment,
 * retry logic, dead-letter queue (DLQ) handling, and stale message reclamation.
 *
 * Subclasses are expected to provide specific implementation details such as stream keys,
 * retry policy, and business logic for processing the messages.
 *
 * @param T The type of payload contained in the stream messages.
 * @param redisClient The Redis client used for interacting with the Redis server.
 */
abstract class AbstractStreamConsumer<T>(
    private val redisClient: RedisClient,
) {
    protected abstract val streamKey: String
    protected abstract val streamMaxLen: Long?
    protected abstract val dlqStreamKey: String
    protected abstract val dlqStreamMaxLen: Long?

    protected abstract val groupName: String
    protected abstract val consumerPrefix: String

    protected abstract val batchSize: Int
    protected abstract val pollBlockMs: Long
    protected abstract val reclaimIdleMs: Long
    protected abstract val reclaimBatchSize: Int

    protected abstract val retryPolicy: RetryPolicy
    protected abstract val serde: StreamSerde<T>

    private val running = AtomicBoolean(false)
    private var executorService: ExecutorService? = null
    private lateinit var consumerName: String

    private companion object {
        val logger: Logger = org.slf4j.LoggerFactory.getLogger(AbstractStreamConsumer::class.java)
        const val DLQ_FIELD_ORIGINAL_STREAM = "originalStream"
        const val DLQ_FIELD_ORIGINAL_MESSAGE_ID = "originalMessageId"
        const val DLQ_FIELD_WRAPPER_JSON = "wrapperJson"
        const val DLQ_FIELD_ERROR_TYPE = "errorType"
        const val DLQ_FIELD_ERROR_MESSAGE = "errorMessage"
        const val DLQ_FIELD_ATTEMPT = "attempt"
        const val DLQ_FIELD_FAILED_AT = "failedAtEpochMs"
    }

    @PostConstruct
    fun init() {
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        consumerName = "$consumerPrefix-${UUID.randomUUID().toString().substring(0, 8)}"
        redisClient.streamCreateGroup(streamKey, groupName)
        executorService = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "$consumerPrefix-consumer").apply { isDaemon = true }
        }
        executorService?.submit(this::consumeLoop)
        logger.info("Stream consumer started: stream={}, group={}, consumer={}", streamKey, groupName, consumerName)
    }

    fun stop() {
        running.set(false)
        executorService?.shutdown()
        logger.info("Stream consumer stopped: stream={}, group={}, consumer={}", streamKey, groupName, if (::consumerName.isInitialized) consumerName else "n/a")
    }

    private fun consumeLoop() {
        while (running.get()) {
            try {
                val records = redisClient.streamReadGroup(streamKey, groupName, consumerName, batchSize, pollBlockMs)
                records.forEach(::processRecord)

                // Reclaim stale pending messages from other consumers.
                val reclaimed = redisClient.streamAutoClaim(
                    streamKey = streamKey,
                    groupName = groupName,
                    consumerName = consumerName,
                    minIdleMs = reclaimIdleMs,
                    count = reclaimBatchSize,
                )
                reclaimed.forEach(::processRecord)
            } catch (e: Exception) {
                if (!running.get()) {
                    return
                }
                logger.error("Stream consume loop failed: stream={}, group={}, err={}", streamKey, groupName, e.message, e)
            }
        }
    }

    private fun processRecord(record: RedisClient.StreamEntry) {
        val wrapper = runCatching { serde.fromFields(record.fields) }
            .getOrElse { error ->
                publishDlq(record.id, record.fields.toString(), error, -1)
                ack(record.id)
                return
            }

        val processing = wrapper.copy(status = MessageStatus.PROCESSING, updatedAtEpochMs = nowEpochMs())
        onProcessing(processing)

        try {
            handleBusiness(processing.payload)
            val completed = processing.copy(status = MessageStatus.COMPLETED, updatedAtEpochMs = nowEpochMs())
            onCompleted(completed)
            ack(record.id)
        } catch (e: Exception) {
            if (retryPolicy.shouldRetry(e, processing.attempt) && processing.attempt < retryPolicy.maxRetry) {
                val retrying = processing.copy(
                    attempt = processing.attempt + 1,
                    status = MessageStatus.RETRYING,
                    updatedAtEpochMs = nowEpochMs(),
                )

                // Critical ordering: ACK only after successful re-publish.
                republish(retrying)
                onRetrying(retrying, e)
                ack(record.id)
                return
            }

            val failed = processing.copy(status = MessageStatus.FAILED, updatedAtEpochMs = nowEpochMs())
            onFailed(failed, e)

            // Critical ordering: ACK only after a successful DLQ publication.
            publishDlq(record.id, serde.toJson(failed), e, failed.attempt)
            ack(record.id)
        }
    }

    private fun republish(wrapper: MessageWrapper<T>) {
        redisClient.streamAdd(streamKey, serde.toFields(wrapper), streamMaxLen)
    }

    private fun publishDlq(originalMessageId: String, wrapperJson: String, error: Throwable, attempt: Int) {
        val deadLetter = DeadLetterMessage(
            originalStream = streamKey,
            originalMessageId = originalMessageId,
            wrapperJson = wrapperJson,
            errorType = error::class.simpleName ?: "UnknownError",
            errorMessage = truncateError(error.message ?: "unknown"),
            attempt = attempt,
            failedAtEpochMs = nowEpochMs(),
        )
        redisClient.streamAdd(dlqStreamKey, toDlqFields(deadLetter), dlqStreamMaxLen)
    }

    private fun toDlqFields(deadLetter: DeadLetterMessage): Map<String, String> =
        mapOf(
            DLQ_FIELD_ORIGINAL_STREAM to deadLetter.originalStream,
            DLQ_FIELD_ORIGINAL_MESSAGE_ID to deadLetter.originalMessageId,
            DLQ_FIELD_WRAPPER_JSON to deadLetter.wrapperJson,
            DLQ_FIELD_ERROR_TYPE to deadLetter.errorType,
            DLQ_FIELD_ERROR_MESSAGE to deadLetter.errorMessage,
            DLQ_FIELD_ATTEMPT to deadLetter.attempt.toString(),
            DLQ_FIELD_FAILED_AT to deadLetter.failedAtEpochMs.toString(),
        )

    private fun ack(messageId: String) {
        runCatching {
            redisClient.streamAck(streamKey, groupName, messageId)
        }.onFailure { e ->
            logger.error("Ack stream message failed: stream={}, group={}, messageId={}, err={}", streamKey, groupName, messageId, e.message, e)
            throw e
        }
    }

    protected open fun onProcessing(wrapper: MessageWrapper<T>) = Unit

    protected open fun onRetrying(wrapper: MessageWrapper<T>, error: Throwable) = Unit

    protected open fun onCompleted(wrapper: MessageWrapper<T>) = Unit

    protected open fun onFailed(wrapper: MessageWrapper<T>, error: Throwable) = Unit

    protected abstract fun handleBusiness(payload: T)

    protected open fun truncateError(error: String): String =
        if (error.length <= 500) error else error.substring(0, 500)

    protected fun nowEpochMs(): Long = System.currentTimeMillis()
}
