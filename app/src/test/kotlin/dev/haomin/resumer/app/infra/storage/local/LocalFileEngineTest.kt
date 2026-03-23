package dev.haomin.resumer.app.infra.storage.local

import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.infra.file.model.FileSource
import dev.haomin.resumer.app.infra.storage.prop.StorageProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Path
import java.time.Duration

class LocalFileEngineTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `engine should upload openUrl download and delete file`() {
        val props = StorageProperties.Local(
            baseDir = tempDir.toString(),
            tempUrlSecret = "local_temp_url_secret_change_me_12345678901234567890",
        )
        val codec = LocalFileUrlTokenCodec(props)
        val engine = LocalFileEngine(props, codec)
        val source = InMemoryFileSource(
            fileName = "resume.txt",
            contentType = "text/plain",
            bytes = "hello resume".toByteArray(),
        )

        val key = engine.uploadFile(source, "resume/account-1")
        assertTrue(key.startsWith("resume/account-1/"))
        assertTrue(key.endsWith(".txt"))
        assertTrue(engine.exist(key))

        val url = engine.openUrl(key, Duration.ofMinutes(1))
        assertTrue(url.startsWith("/api/public/files/local/open?token="))
        val token = url.substringAfter("token=")
        assertEquals(key, codec.decodeReadKey(token))

        val downloaded = engine.download(key)
        val text = downloaded.inputStream().bufferedReader().use { it.readText() }
        assertEquals("hello resume", text)

        assertTrue(engine.delete(key))
        assertFalse(engine.exist(key))
    }

    @Test
    fun `engine should reject invalid prefix traversal`() {
        val props = StorageProperties.Local(
            baseDir = tempDir.toString(),
            tempUrlSecret = "local_temp_url_secret_change_me_12345678901234567890",
        )
        val codec = LocalFileUrlTokenCodec(props)
        val engine = LocalFileEngine(props, codec)
        val source = InMemoryFileSource(
            fileName = "resume.txt",
            contentType = "text/plain",
            bytes = "hello".toByteArray(),
        )

        assertThrows(InvalidParamException::class.java) {
            engine.uploadFile(source, "../escape")
        }
    }

    @Test
    fun `engine should throw not found for missing key download`() {
        val props = StorageProperties.Local(
            baseDir = tempDir.toString(),
            tempUrlSecret = "local_temp_url_secret_change_me_12345678901234567890",
        )
        val codec = LocalFileUrlTokenCodec(props)
        val engine = LocalFileEngine(props, codec)

        assertThrows(NotFoundException::class.java) {
            engine.download("resume/missing.txt")
        }
    }

    private data class InMemoryFileSource(
        override val fileName: String?,
        override val contentType: String?,
        val bytes: ByteArray,
    ) : FileSource {
        override val size: Long = bytes.size.toLong()

        override fun inputStream(): InputStream = ByteArrayInputStream(bytes)
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as InMemoryFileSource

            if (size != other.size) return false
            if (fileName != other.fileName) return false
            if (contentType != other.contentType) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = size.hashCode()
            result = 31 * result + (fileName?.hashCode() ?: 0)
            result = 31 * result + (contentType?.hashCode() ?: 0)
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }
}
