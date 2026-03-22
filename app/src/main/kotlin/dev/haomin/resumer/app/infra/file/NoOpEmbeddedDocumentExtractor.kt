package dev.haomin.resumer.app.infra.file

import org.apache.tika.extractor.EmbeddedDocumentExtractor
import org.apache.tika.metadata.Metadata
import org.slf4j.Logger
import org.xml.sax.ContentHandler
import java.io.InputStream

/**
 * A no-operation implementation of the `EmbeddedDocumentExtractor` interface.
 * This class is used to bypass the processing of embedded documents.
 */
class NoOpEmbeddedDocumentExtractor: EmbeddedDocumentExtractor {

    private companion object {
        val logger: Logger = org.slf4j.LoggerFactory.getLogger(NoOpEmbeddedDocumentExtractor::class.java)
    }

    /**
     * Determines whether an embedded document should be parsed.
     * Always returns false, indicating that embedded documents will be skipped.
     *
     * @param metadata the metadata associated with the embedded document
     * @return false to indicate that the embedded document should not be parsed
     */
    override fun shouldParseEmbedded(metadata: Metadata): Boolean {
        metadata.get("resourceName")
            ?.let { logger.debug("Skip embedded document: {}", it) }
        return false
    }

    override fun parseEmbedded(
        stream: InputStream?,
        handler: ContentHandler?,
        metadata: Metadata?,
        outputHtml: Boolean
    ) {
        logger.debug("Skip embedded document")
    }
}