package dev.haomin.resumer.app.infra.file

import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.infra.file.model.FileSource
import dev.haomin.resumer.app.infra.file.prop.ParserProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class DocumentParserTest {
    private val parser = DocumentParser(
        props = ParserProperties(maxLength = 1024 * 1024),
        cleaner = DocumentCleaner(),
    )

    @Test
    fun `parse should extract and clean text from txt resource`() {
        val resource = checkNotNull(this::class.java.getResourceAsStream("/file/test-resume.txt"))
        val file = InMemoryFileSource(
            fileName = "test-resume.txt",
            contentType = "text/plain",
            bytes = resource.readBytes(),
        )

        val result = parser.parse(file)

        assertFalse(result.contains("image12.png"))
        assertFalse(result.contains("https://cdn.example.com/avatar.jpeg?x=1"))
        assertFalse(result.contains("---"))
        assertEquals(
            "John Doe\n\nExperience: Built backend APIs.\n\nSkills: Kotlin, Spring Boot",
            result
        )
    }

    @Test
    fun `parse should return empty when file size is zero`() {
        val file = object : FileSource {
            override val fileName: String = "empty.txt"
            override val contentType: String = "text/plain"
            override val size: Long = 0L

            override fun inputStream(): InputStream {
                throw IllegalStateException("inputStream should not be called for empty file")
            }
        }

        val result = parser.parse(file)
        assertEquals("", result)
    }

    @Test
    fun `parse should throw AppException when file read fails`() {
        val file = object : FileSource {
            override val fileName: String = "broken.txt"
            override val contentType: String = "text/plain"
            override val size: Long = 12L

            override fun inputStream(): InputStream {
                throw IOException("boom")
            }
        }

        assertThrows(AppException::class.java) { parser.parse(file) }
    }

    private class InMemoryFileSource(
        override val fileName: String?,
        override val contentType: String?,
        private val bytes: ByteArray,
    ) : FileSource {
        override val size: Long = bytes.size.toLong()

        override fun inputStream(): InputStream = ByteArrayInputStream(bytes)
    }
}
