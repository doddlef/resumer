package dev.haomin.resumer.app.infra.file

import dev.haomin.resumer.app.common.exception.AppException
import dev.haomin.resumer.app.infra.file.model.FileSource
import dev.haomin.resumer.app.infra.file.prop.ParserProperties
import org.apache.tika.extractor.EmbeddedDocumentExtractor
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.Parser
import org.apache.tika.parser.pdf.PDFParserConfig
import org.apache.tika.sax.BodyContentHandler
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class DocumentParser(
    private val props: ParserProperties,
    private val cleaner: DocumentCleaner,
) {

    private companion object {
        val logger: Logger = org.slf4j.LoggerFactory.getLogger(DocumentParser::class.java)
    }

    fun parse(file: FileSource): String {
        val fileName = file.fileName
        logger.info("Parsing file: {}", fileName)

        if (file.size == 0L) {
            logger.warn("File is empty: {}", fileName)
            return ""
        }

        return try {
            file.inputStream().use { input ->
                parse(input)
                    .let { cleaner.cleanText(it) }
                    .also { logger.info("File parsed: length={}", it.length) }
            }
        } catch (e: Exception) {
            logger.error("Failed to parse file: {}", e.message, e)
            throw AppException(message = "Failed to parse file")
        }
    }

    /**
     * Parses the input stream to extract textual content using Apache Tika's `AutoDetectParser`.
     * This method processes the input stream while ignoring embedded documents and inline images.
     *
     * @param input the `InputStream` containing the document to be parsed
     * @return the extracted textual content as a `String`
     */
    fun parse(input: InputStream): String {
        val parser = AutoDetectParser()
        val handler = BodyContentHandler(props.maxLength)
        val metadata = Metadata()

        val context = ParseContext()
        context.set(Parser::class.java, parser)
        context.set(EmbeddedDocumentExtractor::class.java, NoOpEmbeddedDocumentExtractor())

        val config = PDFParserConfig()
            .apply { isExtractInlineImages = false }
            .apply { isSortByPosition = false }
        context.set(PDFParserConfig::class.java, config)

        parser.parse(input, handler, metadata, context)
        return handler.toString()
    }
}