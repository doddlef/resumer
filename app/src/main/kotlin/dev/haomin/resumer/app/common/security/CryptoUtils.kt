package dev.haomin.resumer.app.common.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility object providing cryptographic functions such as HMAC generation,
 * Base64 encoding/decoding, and secure string comparison.
 */
object CryptoUtils {
    private const val HMAC_SHA_256 = "HmacSHA256"
    private const val AES = "AES"
    private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12
    private const val TOKEN_VERSION = 1
    private val secureRandom = SecureRandom()

    /**
     * Generates an HMAC-SHA-256 signature for the given input and secret and encodes the result
     * using Base64 URL-safe encoding without padding.
     *
     * @param input The input string to be signed.
     * @param secretBytes The secret key used for generating the HMAC-SHA-256 signature as a byte array.
     * @return The Base64 URL-safe encoded HMAC-SHA-256 signature without padding.
     */
    fun sha256String(input: String, secretBytes: ByteArray): String {
        val mac = Mac.getInstance(HMAC_SHA_256)
        mac.init(SecretKeySpec(secretBytes, HMAC_SHA_256))
        val digest = mac.doFinal(input.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /**
     * Decodes the provided string either as a Base64 encoded string or as a UTF-8 encoded string.
     * If the string is not valid Base64, it defaults to decoding it as UTF-8.
     *
     * @param secret The input string to be decoded.
     * @return A byte array representing the decoded value of the input string.
     */
    fun decodeString(secret: String): ByteArray =
        runCatching { Base64.getDecoder().decode(secret) }
            .getOrElse { secret.toByteArray(StandardCharsets.UTF_8) }

    /**
     * Compares two strings in constant time to determine if they are equal.
     * This method is useful for mitigating timing attacks by ensuring that the comparison
     * time does not vary based on the input provided.
     *
     * @param left the first string to compare, must not be null
     * @param right the second string to compare, can be null
     * @return true if the strings are equal, false otherwise
     */
    fun constantTimeEquals(left: String, right: String?): Boolean {
        if (right == null) {
            return false
        }
        return MessageDigest.isEqual(
            left.toByteArray(StandardCharsets.UTF_8),
            right.toByteArray(StandardCharsets.UTF_8),
        )
    }

    /**
     * Encrypts plaintext using AES-GCM and returns a URL-safe token without padding.
     * Token format before base64url encoding: [version:1 byte][iv:12 bytes][ciphertext+tag]
     */
    fun aesGcmEncryptToUrlToken(plainText: String, secretBytes: ByteArray): String {
        val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, buildAesKey(secretBytes), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        val payload = ByteArray(1 + iv.size + encrypted.size)
        payload[0] = TOKEN_VERSION.toByte()
        System.arraycopy(iv, 0, payload, 1, iv.size)
        System.arraycopy(encrypted, 0, payload, 1 + iv.size, encrypted.size)

        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
    }

    /**
     * Decrypts a token produced by [aesGcmEncryptToUrlToken].
     */
    fun aesGcmDecryptFromUrlToken(token: String, secretBytes: ByteArray): String {
        val payload = runCatching { Base64.getUrlDecoder().decode(token) }
            .getOrElse { throw IllegalArgumentException("invalid token") }
        if (payload.size <= 1 + GCM_IV_BYTES) {
            throw IllegalArgumentException("invalid token")
        }
        val version = payload[0].toInt() and 0xFF
        if (version != TOKEN_VERSION) {
            throw IllegalArgumentException("invalid token version")
        }

        val iv = payload.copyOfRange(1, 1 + GCM_IV_BYTES)
        val encrypted = payload.copyOfRange(1 + GCM_IV_BYTES, payload.size)

        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, buildAesKey(secretBytes), GCMParameterSpec(GCM_TAG_BITS, iv))
        val plainBytes = cipher.doFinal(encrypted)
        return plainBytes.toString(StandardCharsets.UTF_8)
    }

    private fun buildAesKey(secretBytes: ByteArray): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest(secretBytes)
        return SecretKeySpec(digest, AES)
    }
}
