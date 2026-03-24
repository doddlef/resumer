package dev.haomin.resumer.app.framework.redis

import dev.haomin.resumer.app.TestcontainersConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.UUID
import java.util.concurrent.TimeUnit

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class RedisClientIntegrationTest(
    @Autowired private val redisClient: RedisClient,
) {

    data class Profile(
        val name: String,
        val years: Int,
    )

    @Test
    fun `string and object operations work against testcontainer redis`() {
        val key = "test:redis:string:${UUID.randomUUID()}"
        val objKey = "test:redis:obj:${UUID.randomUUID()}"

        redisClient.set(key, "123")
        assertTrue(redisClient.hasKey(key))
        assertEquals("123", redisClient.get(key))

        redisClient.setObj(objKey, Profile("haomin", 2))
        val profile = redisClient.getObj<Profile>(objKey)
        assertNotNull(profile)
        assertEquals("haomin", profile?.name)
        assertEquals(2, profile?.years)

        assertTrue(redisClient.delete(key))
        assertFalse(redisClient.hasKey(key))
        assertNull(redisClient.get(key))
    }

    @Test
    fun `expire and ttl work against testcontainer redis`() {
        val key = "test:redis:ttl:${UUID.randomUUID()}"

        redisClient.set(key, "ttl-value")
        assertTrue(redisClient.expire(key, 2, TimeUnit.SECONDS))

        val ttl = redisClient.getExpire(key, TimeUnit.SECONDS)
        assertNotNull(ttl)
        val ttlValue = ttl ?: error("TTL should not be null right after setting expiration")
        assertTrue(ttlValue in 0..2)

        Thread.sleep(2500)
        assertNull(redisClient.getExpire(key, TimeUnit.SECONDS))
        assertNull(redisClient.get(key))
    }

    @Test
    fun `stream auto claim should reclaim stale pending message`() {
        val streamKey = "test:redis:stream:${UUID.randomUUID()}"
        val group = "g1"
        val consumerA = "c-a"
        val consumerB = "c-b"

        redisClient.streamCreateGroup(streamKey, group)
        redisClient.streamAdd(streamKey, mapOf("k" to "v"))

        // Consume as consumer A without ack so it becomes pending.
        val consumed = redisClient.streamReadGroup(
            streamKey = streamKey,
            groupName = group,
            consumerName = consumerA,
            count = 10,
            blockMs = 100,
        )
        assertEquals(1, consumed.size)

        val reclaimed = redisClient.streamAutoClaim(
            streamKey = streamKey,
            groupName = group,
            consumerName = consumerB,
            minIdleMs = 0,
            count = 10,
        )

        assertEquals(1, reclaimed.size)
        assertEquals(consumed.first().id, reclaimed.first().id)
        assertEquals("v", reclaimed.first().fields["k"])
    }
}
