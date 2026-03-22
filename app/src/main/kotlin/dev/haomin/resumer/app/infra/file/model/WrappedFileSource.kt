package dev.haomin.resumer.app.infra.file.model

import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

/**
 * A concrete implementation of the FileSource interface that wraps a MultipartFile.
 * Provides access to file metadata such as name, content type, and size, and allows
 * opening an InputStream to read the file's contents.
 *
 * @constructor Creates a new WrappedFileSource with the specified MultipartFile.
 * @property file The MultipartFile instance to be wrapped.
 */
class WrappedFileSource(
    val file: MultipartFile,
): FileSource {
    override val fileName: String?
        get() = file.originalFilename

    override val contentType: String?
        get() = file.contentType

    override val size: Long
        get() = file.size

    override fun inputStream(): InputStream =
        file.inputStream
}