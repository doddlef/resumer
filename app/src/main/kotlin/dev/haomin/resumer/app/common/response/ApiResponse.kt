package dev.haomin.resumer.app.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serial
import java.io.Serializable

/**
 * Standard API response structure
 *
 * @param code the response code
 * @param message optional message providing additional information
 * @param payload optional map containing additional data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse(
    val code: Int,
    val message: String? = null,
    val payload: Map<String, Any?>? = null,
): Serializable {

    companion object {
        @field:Serial
        private const val serialVersionUID: Long = 1L

        /** Create a new builder for ApiResponse */
        fun builder(code: ApiCode) = Builder(code)

        /**
         * Create a success response builder
         *
         * @param message optional success message (default is "OK")
         */
        fun success(message: String = "OK") =
            builder(ApiCode.SUCCESS).message(message)

        /**
         * Create a failure response builder
         *
         * @param message optional failure message (default is "Something went wrong")
         */
        fun failure(message: String = "Something went wrong") =
            builder(ApiCode.FAILURE).message(message)
    }

    /**
     * Builder class for constructing ApiResponse objects
     *
     * @param code the response code for the ApiResponse
     */
    class Builder(
        private val code: ApiCode
    ) {
        private var message: String? = null
        private val payload: MutableMap<String, Any?> = mutableMapOf()

        /**
         * Set the message for the ApiResponse
         *
         * @param message the message to set
         */
        fun message(message: String) = apply {
            this.message = message
        }

        /**
         * Add a single key-value pair to the payload
         *
         * @param key the key for the payload entry
         * @param value the value for the payload entry
         */
        fun with(key: String, value: Any?) = apply {
            this.payload[key] = value
        }

        /**
         * Add multiple entries to the payload from a map
         *
         * @param map the map containing entries to add to the payload
         */
        fun withAll(map: Map<String, Any?>) = apply {
            this.payload.putAll(map)
        }

        /**
         * Add a key-value pair to the payload only if a certain condition is true
         *
         * @param key the key for the payload entry
         * @param value the value for the payload entry
         * @param condition the condition that determines whether to add the entry
         */
        fun withIf(key: String, value: Any?, condition: Boolean) = apply {
            if (condition) { this.payload[key] = value }
        }

        /** Build the ApiResponse object */
        fun build(): ApiResponse =
            ApiResponse(
                code = code.code,
                message = message,
                payload = if (payload.isEmpty()) null else payload.toMap(),
            )
    }
}