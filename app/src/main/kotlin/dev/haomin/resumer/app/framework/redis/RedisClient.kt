package dev.haomin.resumer.app.framework.redis

import java.util.concurrent.TimeUnit
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class RedisClient(
    private val template: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        const val KEY_NOT_EXIST = -2L
        const val KEY_NO_EXPIRE = -1L
    }

    private fun ensureNotPipeline() {
        val isPipelined = template.execute { connection -> connection.isPipelined } ?: false
        if (isPipelined) { throw PipelineException() }
    }

    private fun <T> notNullResult(result: T?): T =
        result ?: throw PipelineException()


    /* ---- General Operations ---- */

    /**
     * Set a value to expire in seconds
     *
     * @param key The key to set expiration for
     * @param expire The expiration time in seconds
     * @param unit The time unit for the expiration time (default is seconds)
     * @return True if the expiration was set, false otherwise
     * @throws PipelineException if run in pipeline mode
     */
    fun expire(key: String, expire: Long, unit: TimeUnit = TimeUnit.SECONDS): Boolean =
        run {
            ensureNotPipeline()
            notNullResult(template.expire(key, expire, unit))
        }

    /**
     * Get the remaining time to live of a key
     *
     * @param key The key to check expiration for
     * @param unit The time unit for the expiration time (default is seconds)
     * @return The remaining time to live in the specified unit, null if the key does not exist,
     *         or [KEY_NO_EXPIRE] if the key exists but has no associated expiration
     * @throws PipelineException if run in pipeline mode
     */
    fun getExpire(key: String, unit: TimeUnit = TimeUnit.SECONDS): Long? =
        run {
            ensureNotPipeline()
            notNullResult(template.getExpire(key, unit))
        }.let {
            when(it) {
                KEY_NOT_EXIST -> null
                else -> it
            }
        }

    /**
     * Delete a key
     *
     * @param key The key to delete
     * @return True if the key was deleted, false otherwise
     * @throws PipelineException if run in pipeline mode
     */
    fun delete(key: String): Boolean =
        run {
            ensureNotPipeline()
            notNullResult(template.delete(key))
        }

    /**
     * Check if a key exists
     *
     * @param key The key to check
     * @return True if the key exists, false otherwise
     * @throws PipelineException if run in pipeline mode
     */
    fun hasKey(key: String): Boolean =
        run {
            ensureNotPipeline()
            notNullResult(template.hasKey(key))
        }

    /* ---- String Operations ---- */

    /**
     * Get the value of a key
     *
     * @param key The key to retrieve the value for
     * @return The value associated with the key, or null if not found
     * @throws PipelineException if run in pipeline mode
     */
    fun get(key: String): String? =
        run {
            ensureNotPipeline()
            template.opsForValue().get(key)
        }

    /**
     * Set the value of a key as a string with an optional expiration time
     *
     * @param key The key to set the value for
     * @param value The string value to set
     * @param expire Optional expiration time in seconds
     * @param unit The time unit for the expiration time (default is seconds)
     * @throws PipelineException if run in pipeline mode
     */
    fun set(key: String, value: String, expire: Long? = null, unit: TimeUnit = TimeUnit.SECONDS) {
        ensureNotPipeline()
        if (expire != null && expire > 0)
            template.opsForValue().set(key, value, expire, unit)
        else
            template.opsForValue().set(key, value)
    }

    /**
     * Increment the numeric value of a key by a specified delta
     *
     * @param key The key to increment the value for
     * @param delta The amount to increment by (default is 1)
     * @return The new value after increment
     * @throws PipelineException if run in pipeline mode
     */
    fun increment(key: String, delta: Long = 1L): Long =
        run {
            ensureNotPipeline()
            notNullResult(template.opsForValue().increment(key, delta))
        }

    /* ---- Object Operations ---- */

    /**
     * Set the value of a key with an optional expiration time
     *
     * @param key The key to set the value for
     * @param value The value to set
     * @param expire Optional expiration time in seconds
     * @param unit The time unit for the expiration time (default is seconds)
     * @throws PipelineException if run in pipeline mode
     */
    fun setObj(key: String, value: Any, expire: Long? = null, unit: TimeUnit = TimeUnit.SECONDS) =
        this.set(key, objectMapper.writeValueAsString(value), expire, unit)

    /**
     * Get the value of a key and deserialize it to the specified class
     *
     * @param key The key to retrieve the value for
     * @param asClass The class to deserialize the value into
     * @return The deserialized value associated with the key, or null if not found
     * @throws PipelineException if run in pipeline mode
     */
    fun <T> getObj(key: String, asClass: Class<T>): T? =
        this.get(key)?.let { objectMapper.readValue(it, asClass) }
}

/**
 * Get the value of a key and deserialize it to the target Kotlin type.
 *
 * @param key The key to retrieve the value for
 * @return The deserialized value associated with the key, or null if not found
 * @throws PipelineException if run in pipeline mode
 */
inline fun <reified T> RedisClient.getObj(key: String): T? =
    getObj(key, T::class.java)
