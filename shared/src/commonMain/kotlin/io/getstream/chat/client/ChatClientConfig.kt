package io.getstream.chat.client

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes

/**
 * Configuration for the Stream Chat client.
 */
@Serializable
data class ChatClientConfig(
    val apiKey: String,
    val baseUrl: String,
    val environment: Environment = Environment.PRODUCTION,
    val timeouts: Timeouts = Timeouts(),
    val retryConfig: RetryConfig = RetryConfig(),
    val loggingConfig: LoggingConfig = LoggingConfig(),
    val connectionConfig: ConnectionConfig = ConnectionConfig(),
    val headers: Map<String, String> = emptyMap()
) {
    /**
     * Deployment environment for the Stream Chat API.
     */
    enum class Environment {
        PRODUCTION,
        STAGING,
        DEVELOPMENT
    }

    /**
     * Timeout configuration for various operations.
     */
    @Serializable
    data class Timeouts(
        val connectTimeout: Duration = 30.seconds,
        val socketTimeout: Duration = 30.seconds,
        val requestTimeout: Duration = 30.seconds,
        val keepAliveDuration: Duration = 5.minutes
    )

    /**
     * Retry policy configuration.
     */
    @Serializable
    data class RetryConfig(
        val maxRetries: Int = 3,
        val initialDelay: Duration = 1.seconds,
        val maxDelay: Duration = 30.seconds,
        val factor: Double = 2.0,
        val retryOnStatusCodes: Set<Int> = setOf(408, 429, 500, 502, 503, 504)
    )

    /**
     * Logging configuration.
     */
    @Serializable
    data class LoggingConfig(
        val level: LogLevel = LogLevel.INFO,
        val logNetworkRequests: Boolean = false,
        val logNetworkResponses: Boolean = false,
        val logTokenOperations: Boolean = false,
        val logUserOperations: Boolean = false
    ) {
        enum class LogLevel {
            NONE, ERROR, WARN, INFO, DEBUG, VERBOSE
        }
    }

    /**
     * Connection pooling and management configuration.
     */
    @Serializable
    data class ConnectionConfig(
        val maxConnections: Int = 5,
        val maxConnectionsPerHost: Int = 5,
        val connectionTimeout: Duration = 30.seconds,
        val keepAliveTimeout: Duration = 5.minutes,
        val maxIdleConnections: Int = 5,
        val maxIdleTime: Duration = 5.minutes
    )

    /**
     * SSL/TLS configuration.
     */
    @Serializable
    data class SslConfig(
        val trustAllCerts: Boolean = false,
        val customTrustStore: String? = null,
        val customTrustStorePassword: String? = null
    )

    companion object {
        fun builder() = Builder()

        fun development(apiKey: String) = builder()
            .apiKey(apiKey)
            .baseUrl("https://chat.stream-io-api.com")
            .environment(Environment.DEVELOPMENT)
            .build()

        fun staging(apiKey: String) = builder()
            .apiKey(apiKey)
            .baseUrl("https://chat.staging.stream-io-api.com")
            .environment(Environment.STAGING)
            .build()

        fun production(apiKey: String) = builder()
            .apiKey(apiKey)
            .baseUrl("https://chat.stream-io-api.com")
            .environment(Environment.PRODUCTION)
            .build()
    }

    class Builder {
        private var apiKey: String? = null
        private var baseUrl: String? = null
        private var environment: Environment = Environment.PRODUCTION
        private var timeouts: Timeouts = Timeouts()
        private var retryConfig: RetryConfig = RetryConfig()
        private var loggingConfig: LoggingConfig = LoggingConfig()
        private var connectionConfig: ConnectionConfig = ConnectionConfig()
        private var headers: MutableMap<String, String> = mutableMapOf()

        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
        fun environment(environment: Environment) = apply { this.environment = environment }
        fun timeouts(timeouts: Timeouts) = apply { this.timeouts = timeouts }
        fun retryConfig(retryConfig: RetryConfig) = apply { this.retryConfig = retryConfig }
        fun loggingConfig(loggingConfig: LoggingConfig) = apply { this.loggingConfig = loggingConfig }
        fun connectionConfig(connectionConfig: ConnectionConfig) = apply { this.connectionConfig = connectionConfig }
        fun header(name: String, value: String) = apply { headers[name] = value }

        fun build(): ChatClientConfig {
            require(!apiKey.isNullOrBlank()) { "API key must be set" }
            require(!baseUrl.isNullOrBlank()) { "Base URL must be set" }

            return ChatClientConfig(
                apiKey = apiKey!!,
                baseUrl = baseUrl!!,
                environment = environment,
                timeouts = timeouts,
                retryConfig = retryConfig,
                loggingConfig = loggingConfig,
                connectionConfig = connectionConfig,
                headers = headers.toMap()
            )
        }
    }
} 