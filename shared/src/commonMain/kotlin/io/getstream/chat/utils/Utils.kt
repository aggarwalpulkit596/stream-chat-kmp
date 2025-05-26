package io.getstream.chat.utils

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import io.getstream.chat.serialization.LogLevel

/**
 * TODO: Implement common utilities:
 * - Date/time formatting and parsing
 * - JSON serialization helpers
 * - Error handling utilities
 * - Logging utilities
 * - Platform-specific utilities
 * - Extension functions
 */

object DateTimeUtils {
    fun formatInstant(instant: Instant, pattern: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"): String {
        return instant.toLocalDateTime(TimeZone.UTC).toString()
    }
    
    fun parseInstant(dateString: String): Instant? {
        return try {
            Instant.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}

object JsonUtils {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    inline fun <reified T> fromJson(jsonString: String): T? {
        return try {
            json.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    inline fun <reified T> toJson(value: T): String {
        return json.encodeToString(value)
    }
}

object ErrorUtils {
    sealed class ChatError : Exception() {
        data class NetworkError(override val message: String, override val cause: Throwable? = null) : ChatError()
        data class AuthenticationError(override val message: String) : ChatError()
        data class ValidationError(override val message: String) : ChatError()
        data class ServerError(override val message: String, val code: Int) : ChatError()
        data class UnknownError(override val message: String, override val cause: Throwable? = null) : ChatError()
    }
    
    fun handleError(throwable: Throwable): ChatError {
        return when (throwable) {
            is ChatError -> throwable
            else -> ChatError.UnknownError(throwable.message ?: "Unknown error", throwable)
        }
    }
}

object LogUtils {
    fun log(level: LogLevel, message: String, throwable: Throwable? = null) {
        // TODO: Implement platform-specific logging
        println("[$level] $message${throwable?.let { "\n${it.stackTraceToString()}" } ?: ""}")
    }
}

// Extension functions
fun String.toInstant(): Instant? = DateTimeUtils.parseInstant(this)
fun Instant.toFormattedString(): String = DateTimeUtils.formatInstant(this)

inline fun <reified T> String.fromJson(): T? = JsonUtils.fromJson(this)
inline fun <reified T> T.toJson(): String = JsonUtils.toJson(this) 