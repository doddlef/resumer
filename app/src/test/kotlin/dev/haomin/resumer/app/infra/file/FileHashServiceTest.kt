package dev.haomin.resumer.app.infra.file

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class FileHashServiceTest {
    private val service = FileHashService()

    @Test
    fun `sha256 should produce same value for same input`() {
        val bytes = "same input payload".toByteArray()

        val hashFromBytes = service.sha256(bytes)
        val hashFromStream = service.sha256(ByteArrayInputStream(bytes))
        val hashFromStreamAgain = service.sha256(ByteArrayInputStream(bytes))

        assertEquals(hashFromBytes, hashFromStream)
        assertEquals(hashFromStream, hashFromStreamAgain)
    }

    @Test
    fun `sha256 should produce different value for different input`() {
        val left = "left payload".toByteArray()
        val right = "right payload".toByteArray()

        val leftHash = service.sha256(left)
        val rightHash = service.sha256(right)

        assertNotEquals(leftHash, rightHash)
    }
}
