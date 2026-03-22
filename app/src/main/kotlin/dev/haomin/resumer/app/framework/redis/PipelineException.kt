package dev.haomin.resumer.app.framework.redis

/**
 * [RedisClient] does not support pipeline mode. To use pipeline,
 * please use [org.springframework.data.redis.core.RedisTemplate]
 * or [org.springframework.data.redis.core.StringRedisTemplate] directly.
 *
 * @see RedisClient
 */
class PipelineException : RuntimeException("Redis client does not support pipeline mode.")
