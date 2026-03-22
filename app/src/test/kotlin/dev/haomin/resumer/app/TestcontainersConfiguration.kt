package dev.haomin.resumer.app

import com.redis.testcontainers.RedisStackContainer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	fun postgresContainer(): PostgreSQLContainer {
		return PostgreSQLContainer(DockerImageName.parse("postgres:17"))
	}

	@Bean
	@ServiceConnection(name = "redis")
	fun redisContainer(): RedisStackContainer {
		return RedisStackContainer(DockerImageName.parse("redis/redis-stack:7.4.0-v8"))
	}
}