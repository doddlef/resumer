package dev.haomin.resumer.app.infra.file.model

import java.io.InputStream

/**
 * Represents a source for files, providing essential metadata and a stream for reading the file's contents.
 * Typically used for handling files independent of their physical or logical storage location.
 */
interface FileSource {
    val fileName: String?
    val contentType: String?
    val size: Long

    /**
     * Opens a stream to read the contents of the file represented by this source.
     *
     * @return an InputStream for reading the file's contents
     */
    fun inputStream(): InputStream
}