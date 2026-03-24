package dev.haomin.resumer.app.infra.mq

import dev.haomin.resumer.app.infra.mq.model.MessageWrapper

/**
 * A serializer and deserializer interface for handling the transformation of a message to and from
 * multiple representations such as fields (key-value pairs) or JSON. This interface is designed
 * to operate on messages wrapped in a `MessageWrapper` structure.
 *
 * @param T The type of the payload contained within the `MessageWrapper`.
 */
interface StreamSerde<T> {
    /**
     * Converts the given `MessageWrapper` object into a map of key-value string pairs representing its fields.
     *
     * @param message The `MessageWrapper` instance containing the message data to be transformed.
     * @return A map where the keys are field names of the `MessageWrapper` and the values are their corresponding string representations.
     */
    fun toFields(message: MessageWrapper<T>): Map<String, String>

    /**
     * Constructs a `MessageWrapper` object from the provided map of key-value string pairs,
     * where the map represents the fields and their corresponding values of a message.
     *
     * @param fields A map containing field names as keys and their corresponding string values,
     *               representing the data required to reconstruct a `MessageWrapper` object.
     * @return A `MessageWrapper` instance populated with the parsed field values from the input map.
     */
    fun fromFields(fields: Map<String, String>): MessageWrapper<T>

    /**
     * Converts the given `MessageWrapper` object into its JSON string representation.
     *
     * @param message The `MessageWrapper` instance containing the message data to be serialized into JSON format.
     * @return A JSON-formatted string representing the contents of the `MessageWrapper` object.
     */
    fun toJson(message: MessageWrapper<T>): String
}
