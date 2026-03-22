package dev.haomin.resumer.app.infra.file

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class DocumentCleanerTest {
    private val cleaner = DocumentCleaner()

    @Test
    fun `cleanText should return empty for null or blank input`() {
        assertEquals("", cleaner.cleanText(null))
        assertEquals("", cleaner.cleanText(""))
        assertEquals("", cleaner.cleanText("   \t  "))
    }

    @Test
    fun `cleanText should remove semantic noise and normalize formatting`() {
        val raw =
            "John Doe \t\r\n" +
                "image12.png\n" +
                "https://cdn.example.com/avatar.jpeg?x=1\n" +
                "file:///tmp/tika-123.tmp\n" +
                "---\n" +
                "Experience: Built backend APIs.   \t\n" +
                "\n\n\n" +
                "Skills:\u0007 Kotlin, Spring Boot\r\n"

        val cleaned = cleaner.cleanText(raw)

        assertFalse(cleaned.contains("image12.png"))
        assertFalse(cleaned.contains("https://cdn.example.com/avatar.jpeg?x=1"))
        assertFalse(cleaned.contains("file:///tmp/tika-123.tmp"))
        assertFalse(cleaned.contains("---"))
        assertFalse(cleaned.contains('\u0007'))
        assertFalse(cleaned.contains("\r"))

        assertEquals(
            "John Doe\n\nExperience: Built backend APIs.\n\nSkills: Kotlin, Spring Boot",
            cleaned
        )
    }
}
