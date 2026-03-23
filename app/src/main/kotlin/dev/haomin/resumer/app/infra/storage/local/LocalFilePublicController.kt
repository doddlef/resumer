package dev.haomin.resumer.app.infra.storage.local

import dev.haomin.resumer.app.common.exception.InvalidParamException
import dev.haomin.resumer.app.common.exception.NotFoundException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ConditionalOnProperty(prefix = "resumer.storage", name = ["engine"], havingValue = "local", matchIfMissing = true)
@RequestMapping("/api/public/files/local")
class LocalFilePublicController(
    private val localFileEngine: LocalFileEngine,
    private val tokenCodec: LocalFileUrlTokenCodec,
) {
    private companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(LocalFilePublicController::class.java)
    }

    @GetMapping("/open")
    fun openByToken(@RequestParam("token") token: String): ResponseEntity<InputStreamResource> {
        return try {
            val key = tokenCodec.decodeReadKey(token)
            val file = localFileEngine.download(key)
            val resource = InputStreamResource(file.inputStream())
            val contentType = file.contentType
                ?.takeIf { it.isNotBlank() }
                ?.let { MediaType.parseMediaType(it) }
                ?: MediaType.APPLICATION_OCTET_STREAM

            val headers = HttpHeaders()
            headers.contentType = contentType
            headers.contentLength = file.size
            headers.contentDisposition = ContentDisposition.attachment()
                .filename(file.fileName ?: "download.bin")
                .build()
            headers.cacheControl = "no-store"

            ResponseEntity.ok()
                .headers(headers)
                .body(resource)
        } catch (e: InvalidParamException) {
            logger.debug("Local file token validation failed: {}", e.message)
            throw NotFoundException("file not found")
        } catch (e: NotFoundException) {
            logger.debug("Local file open failed: {}", e.message)
            throw NotFoundException("file not found")
        }
    }
}
