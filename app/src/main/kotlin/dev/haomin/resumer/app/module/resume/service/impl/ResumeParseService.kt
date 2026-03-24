package dev.haomin.resumer.app.module.resume.service.impl

import dev.haomin.resumer.app.infra.file.ContentTypeDetector
import dev.haomin.resumer.app.infra.file.DocumentParser
import dev.haomin.resumer.app.infra.file.model.FileSource
import org.springframework.stereotype.Service

@Service
class ResumeParseService(
    private val parser: DocumentParser,
    private val typeDetector: ContentTypeDetector,
) {

    /**
     * Parse the resume file and extract the content.
     *
     * @return the extracted content as a string, or null if parsing fails.
     */
    fun parseResume(file: FileSource) =
        parser.parse(file)

    /**
     * Detect the content type of the resume file, e.g., PDF, DOCX, etc.
     */
    fun detectContentType(file: FileSource) =
        typeDetector.detect(file)
}
