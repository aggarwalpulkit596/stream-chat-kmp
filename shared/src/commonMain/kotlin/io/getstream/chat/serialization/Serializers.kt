package io.getstream.chat.serialization

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

/**
 * Custom serializers for the Stream Chat SDK.
 */

/**
 * Serializer for kotlinx.datetime.Instant.
 */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

/**
 * Serializer for Map<String, JsonElement> used in extraData fields.
 */
object MapSerializer : KSerializer<Map<String, JsonElement>> {
    override val descriptor = buildClassSerialDescriptor("Map") {
        element<String>("key")
        element<JsonElement>("value")
    }
    
    override fun serialize(encoder: Encoder, value: Map<String, JsonElement>) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("This serializer can only be used with Json format")
        val jsonObject = JsonObject(value)
        jsonEncoder.encodeJsonElement(jsonObject)
    }
    
    override fun deserialize(decoder: Decoder): Map<String, JsonElement> {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("This serializer can only be used with Json format")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        return jsonObject.toMap()
    }
}

/**
 * Serializer for LogLevel enum.
 */
object LogLevelSerializer : KSerializer<LogLevel> {
    override val descriptor = PrimitiveSerialDescriptor("LogLevel", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: LogLevel) {
        encoder.encodeString(value.name.lowercase())
    }
    
    override fun deserialize(decoder: Decoder): LogLevel {
        return try {
            LogLevel.valueOf(decoder.decodeString().uppercase())
        } catch (e: IllegalArgumentException) {
            throw SerializationException("Unknown log level: ${decoder.decodeString()}")
        }
    }
}

/**
 * LogLevel enum for the serializer.
 */
enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
} 