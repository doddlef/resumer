package dev.haomin.resumer.app.infra.file

import org.springframework.stereotype.Service
import java.util.regex.Pattern

/**
 * Provides functionality to clean and normalize text by removing unwanted content and
 * applying formatting conventions. This service focuses on ensuring the resulting text
 * is free from unnecessary noise like control characters, image links, file URLs, and
 * formatting inconsistencies.
 */
@Service
class DocumentCleaner {

    companion object {
        // Match standalone image file name lines such as "image12.png".
        private val IMAGE_FILENAME_LINE: Pattern =
            Pattern.compile("(?m)^image\\d+\\.(png|jpe?g|gif|bmp|webp)\\s*$")

        // Match image URLs (http/https), optional query string, case-insensitive.
        private val IMAGE_URL: Pattern =
            Pattern.compile("https?://\\S+?\\.(png|jpe?g|gif|bmp|webp)(\\?\\S*)?", Pattern.CASE_INSENSITIVE)

        // Match file:// style URLs (for example, temporary parser paths).
        private val FILE_URL: Pattern =
            Pattern.compile("file:(//)?\\S+", Pattern.CASE_INSENSITIVE)

        // Match separator lines like ---, ___, ***, ===.
        private val SEPARATOR_LINE: Pattern =
            Pattern.compile("(?m)^\\s*[-_*=]{3,}\\s*$")

        // Remove control chars while preserving tab and newline.
        private val CONTROL_CHARS: Pattern =
            Pattern.compile("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]")
    }

    fun cleanText(text: String?): String {
        if (text.isNullOrBlank()) return ""
        var cleaned = text

        // Layer 1: semantic noise cleanup.
        cleaned = CONTROL_CHARS.matcher(cleaned).replaceAll("")
        cleaned = IMAGE_FILENAME_LINE.matcher(cleaned).replaceAll("")
        cleaned = IMAGE_URL.matcher(cleaned).replaceAll("")
        cleaned = FILE_URL.matcher(cleaned).replaceAll("")
        cleaned = SEPARATOR_LINE.matcher(cleaned).replaceAll("")

        // Layer 2: formatting normalization.
        cleaned = cleaned.replace("\r\n", "\n").replace("\r", "\n")
        cleaned = cleaned.replace(Regex("(?m)[ \\t]+$"), "")
        cleaned = cleaned.replace(Regex("\\n{3,}"), "\n\n")

        return cleaned.trim()
    }
}
