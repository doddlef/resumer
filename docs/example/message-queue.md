# Message Queue Pseudocode (Redis Stream)

This document defines abstract pseudocode for a reusable Redis Stream message queue pattern.
It uses:
- `abstract val` for stream/group/consumer configuration
- a `MessageWrapper<T>` to centralize retry/status metadata
- `StreamSerde<T>` that explicitly serializes/deserializes `MessageWrapper<T>`
- a concrete calculator example (`a + b`)

## Goals
- Reuse one pattern for multiple async domains.
- Keep at-least-once delivery semantics.
- Standardize retry, DLQ, and pending recovery.
- Keep business logic in domain handlers.

## Core Models

```kotlin
enum class MessageStatus {
    PENDING,
    PROCESSING,
    RETRYING,
    COMPLETED,
    FAILED,
    DEAD_LETTER,
}

data class MessageWrapper<T>(
    val id: String,                  // business id / dedupe key
    val type: String,                // message type
    val payload: T,                  // business payload
    val attempt: Int = 0,
    val status: MessageStatus = MessageStatus.PENDING,
    val traceId: String,             // correlation id across logs/services
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

data class DeadLetterMessage(
    val originalStream: String,
    val originalMessageId: String,
    val wrapperJson: String,
    val errorType: String,
    val errorMessage: String,
    val attempt: Int,
    val failedAtEpochMs: Long,
)
```

## Queue Contracts

```kotlin
interface StreamSerde<T> {
    fun toFields(message: MessageWrapper<T>): Map<String, String>
    fun fromFields(fields: Map<String, String>): MessageWrapper<T>
    fun toJson(message: MessageWrapper<T>): String
}

interface RetryPolicy {
    fun shouldRetry(error: Throwable, attempt: Int): Boolean
    val maxRetry: Int
}

interface DlqPublisher {
    fun publish(dlqStream: String, deadLetter: DeadLetterMessage): String
}

interface StreamOps {
    fun add(stream: String, fields: Map<String, String>, maxLen: Long? = null): String
    fun ack(stream: String, group: String, messageId: String)
    fun createGroup(stream: String, group: String)

    fun readGroup(
        stream: String,
        group: String,
        consumer: String,
        count: Int,
        blockMs: Long,
    ): List<StreamRecord>

    fun autoClaim(
        stream: String,
        group: String,
        consumer: String,
        minIdleMs: Long,
        count: Int,
    ): List<StreamRecord>
}

data class StreamRecord(
    val messageId: String,
    val fields: Map<String, String>,
)
```

## Abstract Publisher

```kotlin
abstract class AbstractStreamPublisher<T>(
    private val streamOps: StreamOps,
) {
    abstract val streamKey: String
    abstract val streamMaxLen: Long?
    abstract val serde: StreamSerde<T>

    fun publish(wrapper: MessageWrapper<T>): String {
        return try {
            val messageId = streamOps.add(
                stream = streamKey,
                fields = serde.toFields(wrapper),
                maxLen = streamMaxLen,
            )
            onPublished(wrapper, messageId)
            messageId
        } catch (e: Exception) {
            onPublishFailed(wrapper, e)
            throw e
        }
    }

    protected open fun onPublished(wrapper: MessageWrapper<T>, messageId: String) {}
    protected open fun onPublishFailed(wrapper: MessageWrapper<T>, error: Throwable) {}
}
```

## Abstract Consumer

```kotlin
abstract class AbstractStreamConsumer<T>(
    private val streamOps: StreamOps,
    private val retryPolicy: RetryPolicy,
    private val dlqPublisher: DlqPublisher,
) {
    abstract val streamKey: String
    abstract val dlqStreamKey: String
    abstract val groupName: String
    abstract val consumerName: String

    abstract val batchSize: Int
    abstract val pollBlockMs: Long
    abstract val reclaimIdleMs: Long
    abstract val reclaimBatchSize: Int

    abstract val serde: StreamSerde<T>

    fun start() {
        streamOps.createGroup(streamKey, groupName)
        runConsumeLoop()
    }

    fun stop() {
        // set running=false in real implementation
    }

    private fun runConsumeLoop() {
        while (isRunning()) {
            val newRecords = streamOps.readGroup(
                stream = streamKey,
                group = groupName,
                consumer = consumerName,
                count = batchSize,
                blockMs = pollBlockMs,
            )
            newRecords.forEach { processRecord(it) }

            val reclaimed = streamOps.autoClaim(
                stream = streamKey,
                group = groupName,
                consumer = consumerName,
                minIdleMs = reclaimIdleMs,
                count = reclaimBatchSize,
            )
            reclaimed.forEach { processRecord(it) }
        }
    }

    private fun processRecord(record: StreamRecord) {
        val wrapper = try {
            serde.fromFields(record.fields)
        } catch (e: Exception) {
            publishDlq(record, rawPayload = record.fields.toString(), error = e, attempt = -1)
            streamOps.ack(streamKey, groupName, record.messageId)
            return
        }

        val processing = wrapper.copy(
            status = MessageStatus.PROCESSING,
            updatedAtEpochMs = nowEpochMs(),
        )
        onProcessing(processing)

        try {
            handleBusiness(processing.payload)

            val completed = processing.copy(
                status = MessageStatus.COMPLETED,
                updatedAtEpochMs = nowEpochMs(),
            )
            onCompleted(completed)
            streamOps.ack(streamKey, groupName, record.messageId)
        } catch (e: Exception) {
            val shouldRetry = retryPolicy.shouldRetry(e, processing.attempt)

            if (shouldRetry && processing.attempt < retryPolicy.maxRetry) {
                val retrying = processing.copy(
                    attempt = processing.attempt + 1,
                    status = MessageStatus.RETRYING,
                    updatedAtEpochMs = nowEpochMs(),
                )

                // Important: ACK only after republish succeeds.
                republish(retrying)
                onRetrying(retrying, e)
                streamOps.ack(streamKey, groupName, record.messageId)
            } else {
                val failed = processing.copy(
                    status = MessageStatus.FAILED,
                    updatedAtEpochMs = nowEpochMs(),
                )
                onFailed(failed, e)

                publishDlq(
                    record = record,
                    rawPayload = serde.toJson(failed),
                    error = e,
                    attempt = failed.attempt,
                )
                streamOps.ack(streamKey, groupName, record.messageId)
            }
        }
    }

    private fun publishDlq(record: StreamRecord, rawPayload: String, error: Throwable, attempt: Int) {
        val deadLetter = DeadLetterMessage(
            originalStream = streamKey,
            originalMessageId = record.messageId,
            wrapperJson = rawPayload,
            errorType = error::class.simpleName ?: "UnknownError",
            errorMessage = error.message ?: "unknown",
            attempt = attempt,
            failedAtEpochMs = nowEpochMs(),
        )
        dlqPublisher.publish(dlqStreamKey, deadLetter)
    }

    protected abstract fun isRunning(): Boolean
    protected abstract fun republish(wrapper: MessageWrapper<T>)
    protected abstract fun handleBusiness(payload: T)

    protected open fun onProcessing(wrapper: MessageWrapper<T>) {}
    protected open fun onRetrying(wrapper: MessageWrapper<T>, error: Throwable) {}
    protected open fun onCompleted(wrapper: MessageWrapper<T>) {}
    protected open fun onFailed(wrapper: MessageWrapper<T>, error: Throwable) {}

    protected fun nowEpochMs(): Long = System.currentTimeMillis()
}
```

## Concrete Example: Calculator Service

### Business payload

```kotlin
data class AddNumbersPayload(
    val a: Int,
    val b: Int,
)
```

### StreamSerde implementation

```kotlin
class CalculatorAddSerde(
    private val objectMapper: ObjectMapper,
) : StreamSerde<AddNumbersPayload> {

    override fun toFields(message: MessageWrapper<AddNumbersPayload>): Map<String, String> {
        return mapOf(
            "id" to message.id,
            "type" to message.type,
            "attempt" to message.attempt.toString(),
            "status" to message.status.name,
            "traceId" to message.traceId,
            "createdAtEpochMs" to message.createdAtEpochMs.toString(),
            "updatedAtEpochMs" to message.updatedAtEpochMs.toString(),
            "payload" to objectMapper.writeValueAsString(message.payload),
        )
    }

    override fun fromFields(fields: Map<String, String>): MessageWrapper<AddNumbersPayload> {
        val payload = objectMapper.readValue(fields.getValue("payload"), AddNumbersPayload::class.java)
        return MessageWrapper(
            id = fields.getValue("id"),
            type = fields.getValue("type"),
            payload = payload,
            attempt = fields.getValue("attempt").toInt(),
            status = MessageStatus.valueOf(fields.getValue("status")),
            traceId = fields.getValue("traceId"),
            createdAtEpochMs = fields.getValue("createdAtEpochMs").toLong(),
            updatedAtEpochMs = fields.getValue("updatedAtEpochMs").toLong(),
        )
    }

    override fun toJson(message: MessageWrapper<AddNumbersPayload>): String {
        return objectMapper.writeValueAsString(message)
    }
}
```

### Publisher

```kotlin
class CalculatorAddPublisher(
    streamOps: StreamOps,
    override val serde: StreamSerde<AddNumbersPayload>,
) : AbstractStreamPublisher<AddNumbersPayload>(streamOps) {

    override val streamKey: String = "demo.calculator.add.v1"
    override val streamMaxLen: Long? = 10_000L

    fun publishAdd(a: Int, b: Int, traceId: String): String {
        val now = System.currentTimeMillis()
        val wrapper = MessageWrapper(
            id = "calc-$traceId",
            type = "calculator_add",
            payload = AddNumbersPayload(a, b),
            attempt = 0,
            status = MessageStatus.PENDING,
            traceId = traceId,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
        return publish(wrapper)
    }
}
```

### Consumer

```kotlin
class CalculatorAddConsumer(
    streamOps: StreamOps,
    retryPolicy: RetryPolicy,
    dlqPublisher: DlqPublisher,
    private val publisher: CalculatorAddPublisher,
    override val serde: StreamSerde<AddNumbersPayload>,
) : AbstractStreamConsumer<AddNumbersPayload>(streamOps, retryPolicy, dlqPublisher) {

    override val streamKey: String = "demo.calculator.add.v1"
    override val dlqStreamKey: String = "demo.calculator.add.v1.dlq"
    override val groupName: String = "calculator-workers"
    override val consumerName: String = "calculator-worker-01"

    override val batchSize: Int = 10
    override val pollBlockMs: Long = 5000
    override val reclaimIdleMs: Long = 60_000
    override val reclaimBatchSize: Int = 10

    override fun isRunning(): Boolean = true

    override fun republish(wrapper: MessageWrapper<AddNumbersPayload>) {
        publisher.publish(wrapper)
    }

    override fun handleBusiness(payload: AddNumbersPayload) {
        val answer = payload.a + payload.b
        println("calculator result: ${payload.a} + ${payload.b} = $answer")
    }

    override fun onFailed(wrapper: MessageWrapper<AddNumbersPayload>, error: Throwable) {
        println("calculator job failed: id=${wrapper.id}, attempt=${wrapper.attempt}, err=${error.message}")
    }
}
```

### Example usage

```kotlin
fun main() {
    val publisher: CalculatorAddPublisher = TODO()
    val consumer: CalculatorAddConsumer = TODO()

    consumer.start()

    publisher.publishAdd(a = 7, b = 13, traceId = "t-001")
    // Expected output from consumer:
    // calculator result: 7 + 13 = 20
}
```

## Notes
- Delivery semantics: at-least-once.
- Handler must be idempotent if side effects exist.
- Never ACK before retry republish or DLQ publish succeeds.
- `MessageWrapper.status` is process metadata, not your final business source of truth.
