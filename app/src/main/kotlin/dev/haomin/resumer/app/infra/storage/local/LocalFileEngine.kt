package dev.haomin.resumer.app.infra.storage.local

import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.exception.NotFoundException
import dev.haomin.resumer.app.infra.file.model.FileSource
import dev.haomin.resumer.app.infra.storage.FileEngine
import dev.haomin.resumer.app.infra.storage.prop.StorageProperties
import org.slf4j.Logger
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

class LocalFileEngine(
    private val properties: StorageProperties.Local,
    private val tokenCodec: LocalFileUrlTokenCodec,
) : FileEngine {
    private val baseDir: Path = Path.of(properties.baseDir).toAbsolutePath().normalize()

    private companion object {
        val logger: Logger = org.slf4j.LoggerFactory.getLogger(LocalFileEngine::class.java)
    }

    init {
        Files.createDirectories(baseDir)
    }

    override fun uploadFile(file: FileSource, prefix: String): String {
        val cleanedPrefix = normalizePrefix(prefix)
        val extension = extractExtension(file.fileName)
        val storedName = "${UUID.randomUUID()}$extension"
        val key = if (cleanedPrefix.isBlank()) storedName else "$cleanedPrefix/$storedName"
        val target = resolveKeyPath(key)
        Files.createDirectories(target.parent)

        file.inputStream().use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
        logger.info("Stored local file key={} path={}", key, target)
        return key
    }

    override fun openUrl(key: String, ttl: java.time.Duration): String {
        if (!exist(key)) throw NotFoundException("file not found")
        val token = tokenCodec.encodeReadKey(normalizeKey(key), ttl)
        val relativeUrl = "/api/public/files/local/open?token=$token"
        val base = properties.publicBaseUrl?.trim()?.removeSuffix("/")
        return if (base.isNullOrBlank()) relativeUrl else "$base$relativeUrl"
    }

    override fun delete(key: String): Boolean {
        val path = resolveKeyPath(key)
        return Files.deleteIfExists(path)
    }

    override fun exist(key: String): Boolean = Files.exists(resolveKeyPath(key))

    override fun download(key: String): FileSource {
        val cleanKey = normalizeKey(key)
        val path = resolveKeyPath(cleanKey)
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw NotFoundException("file not found")
        }
        return LocalFileSource(
            path = path,
            fileName = path.fileName.toString(),
            size = Files.size(path),
            contentType = Files.probeContentType(path),
        )
    }

    private fun resolveKeyPath(key: String): Path {
        val normalized = normalizeKey(key)
        val resolved = baseDir.resolve(normalized).normalize()
        if (!resolved.startsWith(baseDir)) {
            throw InvalidParamException("invalid file key")
        }
        return resolved
    }

    private fun normalizePrefix(prefix: String): String {
        if (prefix.isBlank()) {
            return ""
        }
        val normalized = prefix.trim().replace('\\', '/').trim('/')
        if (normalized.isBlank()) {
            return ""
        }
        val cleanPath = Path.of(normalized).normalize().toString().replace('\\', '/')
        if (cleanPath == "." || cleanPath.startsWith("..") || cleanPath.contains("/../")) {
            throw InvalidParamException("invalid file prefix")
        }
        return cleanPath
    }

    private fun normalizeKey(key: String): String {
        val normalized = key.trim().replace('\\', '/').trim('/')
        if (normalized.isBlank()) {
            throw InvalidParamException("invalid file key")
        }
        val cleanPath = Path.of(normalized).normalize().toString().replace('\\', '/')
        if (cleanPath == "." || cleanPath.startsWith("..") || cleanPath.contains("/../")) {
            throw InvalidParamException("invalid file key")
        }
        return cleanPath
    }

    private fun extractExtension(fileName: String?): String {
        if (fileName.isNullOrBlank()) {
            return ""
        }
        val name = fileName.substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
        val suffix = name.substringAfterLast('.', "")
            .lowercase()
        if (suffix.isBlank() || suffix.length > 10) {
            return ""
        }
        if (!suffix.all { it.isLetterOrDigit() }) {
            return ""
        }
        return ".$suffix"
    }

    private data class LocalFileSource(
        val path: Path,
        override val fileName: String?,
        override val contentType: String?,
        override val size: Long,
    ) : FileSource {
        override fun inputStream(): InputStream = Files.newInputStream(path)
    }
}
