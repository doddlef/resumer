package dev.haomin.resumer.app.infra.file

import dev.haomin.resumer.app.infra.file.model.FileSource
import org.springframework.stereotype.Service
import java.io.InputStream
import java.security.MessageDigest

/**
 * FileHashService is responsible for calculating the hash of a file using the SHA-256 algorithm.
 * It provides methods to compute the hash from a FileSource, an InputStream, or a byte array.
 */
@Service
class FileHashService {

    /**
     * Calculates the SHA-256 hash of a file represented by a FileSource.
     */
    fun sha256(file: FileSource): String =
        file.inputStream().use { sha256(it) }

    /**
     * Calculates the SHA-256 hash of a file from an InputStream.
     * It reads the input stream in chunks to efficiently compute the hash without loading the entire file into memory.
     */
    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance(SHA_256)
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().toHexLowercase()
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance(SHA_256)
            .digest(bytes)
            .toHexLowercase()

    private fun ByteArray.toHexLowercase(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {
        const val SHA_256 = "SHA-256"
        const val BUFFER_SIZE = 16 * 1024
    }
}
