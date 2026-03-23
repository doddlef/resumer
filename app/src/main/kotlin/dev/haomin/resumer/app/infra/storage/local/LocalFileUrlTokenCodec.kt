package dev.haomin.resumer.app.infra.storage.local

import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.security.CryptoUtils
import dev.haomin.resumer.app.infra.storage.prop.StorageProperties
import java.time.Duration
import java.time.Instant
import java.util.Base64

class LocalFileUrlTokenCodec(
    properties: StorageProperties.Local,
) {
    private val secretBytes = CryptoUtils.decodeString(properties.tempUrlSecret)

    fun encodeReadKey(key: String, ttl: Duration): String {
        val expiryEpochSeconds = Instant.now().plus(ttl).epochSecond
        val keyPayload = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(key.toByteArray(Charsets.UTF_8))
        val payload = "$CLAIM_TYPE_READ|$expiryEpochSeconds|$keyPayload"
        return CryptoUtils.aesGcmEncryptToUrlToken(payload, secretBytes)
    }

    fun decodeReadKey(token: String): String =
        try {
            val payload = CryptoUtils.aesGcmDecryptFromUrlToken(token, secretBytes)
            val parts = payload.split('|', limit = 3)
            if (parts.size != 3) {
                throw InvalidParamException("invalid file token")
            }

            val type = parts[0]
            val expiryEpochSeconds = parts[1].toLongOrNull()
                ?: throw InvalidParamException("invalid file token")
            val key = runCatching {
                String(Base64.getUrlDecoder().decode(parts[2]), Charsets.UTF_8)
            }.getOrElse { throw InvalidParamException("invalid file token") }

            if (type != CLAIM_TYPE_READ) {
                throw InvalidParamException("invalid file token")
            }
            if (expiryEpochSeconds <= Instant.now().epochSecond) {
                throw InvalidParamException("file url has expired")
            }
            if (key.isBlank()) {
                throw InvalidParamException("invalid file token")
            }
            key
        } catch (e: InvalidParamException) {
            throw e
        } catch (_: Exception) {
            throw InvalidParamException("invalid file token")
        }

    private companion object {
        const val CLAIM_TYPE_READ = "local_file_read"
    }
}
