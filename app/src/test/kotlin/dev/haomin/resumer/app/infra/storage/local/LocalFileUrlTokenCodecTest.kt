package dev.haomin.resumer.app.infra.storage.local

import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.infra.storage.prop.StorageProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration

class LocalFileUrlTokenCodecTest {
    private val codec = LocalFileUrlTokenCodec(
        StorageProperties.Local(
            tempUrlSecret = "local_temp_url_secret_change_me_12345678901234567890",
        )
    )

    @Test
    fun `decodeReadKey should return original key for valid token`() {
        val key = "resume/123/file.txt"
        val token = codec.encodeReadKey(key, Duration.ofMinutes(1))

        val decoded = codec.decodeReadKey(token)

        assertEquals(key, decoded)
    }

    @Test
    fun `decodeReadKey should throw when token is expired`() {
        val token = codec.encodeReadKey("resume/123/file.txt", Duration.ofSeconds(-1))

        assertThrows(InvalidParamException::class.java) {
            codec.decodeReadKey(token)
        }
    }

    @Test
    fun `decodeReadKey should throw when token is tampered`() {
        val token = codec.encodeReadKey("resume/123/file.txt", Duration.ofMinutes(1))
        val tampered = token.dropLast(1) + if (token.last() == 'a') "b" else "a"

        assertThrows(InvalidParamException::class.java) {
            codec.decodeReadKey(tampered)
        }
    }
}
