package dev.haomin.resumer.app.framework.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.ObjectMapper

@Configuration
class RedisConfiguration {

    @Bean("redisTemplate")
    fun redisTemplate(
        factory: RedisConnectionFactory, objectMapper: ObjectMapper,
    ): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>().apply {
            connectionFactory = factory

            val strSerializer = StringRedisSerializer()
            val jsonSerializer = GenericJacksonJsonRedisSerializer(objectMapper)

            keySerializer = strSerializer
            valueSerializer = jsonSerializer

            hashKeySerializer = strSerializer
            hashValueSerializer = jsonSerializer

            afterPropertiesSet()
        }

    @Bean("stringRedisTemplate")
    fun stringRedisTemplate(factory: RedisConnectionFactory): StringRedisTemplate =
        StringRedisTemplate(factory)
}
