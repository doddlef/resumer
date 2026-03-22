package dev.haomin.resumer.app.infra.file

import io.jsonwebtoken.io.IOException
import org.apache.tika.Tika
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class ContentTypeDetector {

    private val tika = Tika()

    private companion object {
        val logger: Logger = org.slf4j.LoggerFactory.getLogger(ContentTypeDetector::class.java)

        const val DEFAULT_CONTENT_TYPE = "application/octet-stream"
    }

    /**
     * Detects the MIME type of given file using Apache Tika. If detection fails, it falls back to the
     * Content-Type provided by the file or a default value of "application/octet-stream".
     *
     * @param file the `FileSource` representing the file whose MIME type needs to be detected
     * @return the detected MIME type of the file, or a fallback MIME type if detection fails
     */
    fun detect(file: FileSource): String =
        try {
            file.inputStream().use {
                tika.detect(it, file.fileName)
            }
        } catch (e: IOException) {
            logger.warn("Failed to check file type, fallback to Content-Type: {}", e.message)
            file.contentType ?: DEFAULT_CONTENT_TYPE
        }
}