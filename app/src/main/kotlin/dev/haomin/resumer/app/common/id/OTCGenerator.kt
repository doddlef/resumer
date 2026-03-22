package dev.haomin.resumer.app.common.id

import org.springframework.stereotype.Component
import java.security.SecureRandom

/**
 * Represents a generator for producing one-time codes (OTC) of a specified length.
 * This interface is designed for use cases where temporary strings, such as
 * validation codes or tokens, are required.
 */
interface OTCGenerator {

    /**
     * Generates a one-time code (OTC) with the specified length.
     *
     * @param length The desired length of the one-time code to be generated.
     * @return A string representing the generated one-time code.
     */
    fun next(length: Int): String
}

/**
 * Implementation of the [OTCGenerator] interface that generates numeric one-time codes (OTC).
 * This generator produces a string containing random digits between 0 and 9 of the specified length.
 *
 * Uses a cryptographically secure random number generator ([SecureRandom]) to ensure the unpredictability
 * of the generated codes.
 *
 * This class is designed for use cases requiring numeric one-time codes, such as authentication tokens
 * or verification codes.
 */
@Component
class NumericGenerator: OTCGenerator {

    private val random: SecureRandom = SecureRandom()

    override fun next(length: Int): String =
        buildString(length) {
            repeat(length) {
                append(random.nextInt(10))
            }
        }
}