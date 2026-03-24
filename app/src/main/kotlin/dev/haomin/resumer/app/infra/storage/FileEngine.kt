package dev.haomin.resumer.app.infra.storage

import dev.haomin.resumer.app.infra.file.model.FileSource
import java.time.Duration

/**
 * Defines operations for managing files in a storage system, such as uploading, downloading,
 * generating presigned URLs, checking existence, and deleting files.
 * Provides abstraction for working with file handling systems.
 */
interface FileEngine {

    /**
     * Uploads a file to a storage destination with a specified prefix.
     *
     * @param file the file source to be uploaded, providing file metadata and a stream for reading its contents
     * @param prefix the prefix to be applied to the file's storage path
     * @return the file key
     */
    fun uploadFile(file: FileSource, prefix: String): String

    /**
     * Generates a presigned URL for accessing a file with the given key, valid for the specified time-to-live (TTL).
     *
     * @param key the identifier of the file to generate the URL for
     * @param ttl the duration for which the generated URL remains valid
     * @return a presigned URL as a string
     */
    fun openUrl(key: String, ttl: Duration): String

    /**
     * Deletes a file associated with the given key from the storage.
     *
     * @param key the identifier of the file to be deleted
     * @return true if the file was successfully deleted, false otherwise
     */
    fun delete(key: String): Boolean

    /**
     * Checks whether a file associated with the given key exists in the storage.
     *
     * @param key the identifier of the file to check for existence
     * @return true if the file exists, false otherwise
     */
    fun exist(key: String): Boolean

    /**
     * Downloads a file associated with the specified key from the storage.
     *
     * @param key the identifier of the file to be downloaded
     * @return a FileSource representing the downloaded file, including its metadata and a stream for reading its contents
     */
    fun download(key: String): FileSource
}