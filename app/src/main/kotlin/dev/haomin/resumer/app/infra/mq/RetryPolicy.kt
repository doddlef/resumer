package dev.haomin.resumer.app.infra.mq

/**
 * Defines a contract for implementing retry policies in cases of errors during an operation.
 *
 * A `RetryPolicy` helps determine whether an operation should be retried based on the type of
 * error encountered and the number of retry attempts already performed.
 */
interface RetryPolicy {
    val maxRetry: Int

    fun shouldRetry(error: Throwable, attempt: Int): Boolean
}

/**
 * A fixed retry policy implementation of the `RetryPolicy` interface.
 *
 * This class provides logic to determine whether an operation should be retried
 * based on a fixed maximum number of retry attempts.
 *
 * @param maxRetry The maximum number of retry attempts allowed before giving up.
 */
class FixedRetryPolicy(
    override val maxRetry: Int,
) : RetryPolicy {
    override fun shouldRetry(error: Throwable, attempt: Int): Boolean =
        attempt < maxRetry
}
