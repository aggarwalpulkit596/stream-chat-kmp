package io.getstream.chat.client

import io.getstream.chat.models.ApiError
import io.getstream.chat.models.ApiException
import io.getstream.chat.models.ApiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration

/**
 * Configuration for the HTTP client.
 *
 * @property baseUrl The base URL for all requests
 * @property timeout The timeout configuration for requests
 * @property retry The retry configuration for failed requests
 * @property defaultHeaders Default headers to include in all requests
 */
@Serializable
data class HttpClientConfig(
    val baseUrl: String,
    val timeout: TimeoutConfig = TimeoutConfig(),
    val retry: RetryConfig = RetryConfig(),
    val defaultHeaders: Map<String, String> = emptyMap()
)

/**
 * Configuration for request timeouts.
 *
 * @property connect Timeout for establishing connection
 * @property read Timeout for reading response
 * @property write Timeout for writing request
 */
@Serializable
data class TimeoutConfig(
    val connect: Duration = Duration.parse("30s"),
    val read: Duration = Duration.parse("30s"),
    val write: Duration = Duration.parse("30s")
)

/**
 * Configuration for retrying failed requests.
 *
 * @property maxAttempts Maximum number of retry attempts
 * @property initialDelay Initial delay before first retry
 * @property maxDelay Maximum delay between retries
 * @property factor Multiplier for delay between retries
 * @property retryOnStatusCodes HTTP status codes that should trigger a retry
 */
@Serializable
data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = Duration.parse("1s"),
    val maxDelay: Duration = Duration.parse("10s"),
    val factor: Double = 2.0,
    val retryOnStatusCodes: Set<Int> = setOf(408, 429, 500, 502, 503, 504)
)

/**
 * Represents an HTTP request to be made by the client.
 *
 * @property method The HTTP method
 * @property path The request path (will be appended to baseUrl)
 * @property headers Additional headers for this request
 * @property queryParams Query parameters to append to the URL
 * @property body The request body
 */
data class HttpRequest(
    val method: HttpMethod,
    val path: String,
    val headers: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap(),
    val body: RequestBody? = null
)

/**
 * Represents an HTTP response from the server.
 *
 * @property statusCode The HTTP status code
 * @property headers The response headers
 * @property body The response body
 */
data class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as HttpResponse
        return statusCode == other.statusCode &&
            headers == other.headers &&
            body.contentEquals(other.body)
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}

/**
 * Represents the body of an HTTP request.
 * Can be one of several types depending on the content.
 */
sealed class RequestBody {
    /**
     * Empty request body
     */
    object Empty : RequestBody()

    /**
     * JSON request body
     *
     * @property json The JSON string
     */
    data class Json(val json: String) : RequestBody()

    /**
     * Form data request body
     *
     * @property formData The form data as key-value pairs
     */
    data class Form(val formData: Map<String, String>) : RequestBody()

    /**
     * Multipart request body for file uploads
     *
     * @property parts The parts of the multipart request
     */
    data class Multipart(val parts: List<Part>) : RequestBody()
}

/**
 * Represents a part in a multipart request.
 */
sealed class Part {
    /**
     * A form field part
     *
     * @property name The field name
     * @property value The field value
     */
    data class FormField(
        val name: String,
        val value: String
    ) : Part()

    /**
     * A file part
     *
     * @property name The field name
     * @property filename The name of the file
     * @property contentType The MIME type of the file
     * @property content The file content
     */
    data class File(
        val name: String,
        val filename: String,
        val contentType: String,
        val content: ByteArray
    ) : Part() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as File
            return name == other.name &&
                filename == other.filename &&
                contentType == other.contentType &&
                content.contentEquals(other.content)
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + filename.hashCode()
            result = 31 * result + contentType.hashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }
    }
}

/**
 * HTTP methods supported by the client.
 */
enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}

/**
 * Exception thrown when an HTTP request fails.
 *
 * @property statusCode The HTTP status code
 * @property message The error message
 * @property response The response body if available
 */
class HttpException(
    val statusCode: Int,
    override val message: String,
    val response: ByteArray? = null
) : Exception(message)

/**
 * Platform-agnostic HTTP client interface for the Stream Chat SDK.
 * This interface defines the core HTTP operations that must be implemented
 * by platform-specific clients.
 */
interface ChatHttpClient {
    /**
     * The configuration for this HTTP client.
     */
    val config: HttpClientConfig

    /**
     * Executes an HTTP request and returns the response.
     *
     * @param request The HTTP request to execute
     * @return The HTTP response
     * @throws HttpException if the request fails
     * @throws ApiException if the response contains an API error
     */
    suspend fun execute(request: HttpRequest): HttpResponse

    /**
     * Executes an HTTP request and returns the response as a flow of chunks.
     * Useful for streaming responses or large file downloads.
     *
     * @param request The HTTP request to execute
     * @return A flow of response chunks
     * @throws HttpException if the request fails
     * @throws ApiException if the response contains an API error
     */
    fun executeAsFlow(request: HttpRequest): Flow<ByteArray>

    /**
     * Convenience method for making a GET request.
     *
     * @param path The request path
     * @param headers Additional headers
     * @param queryParams Query parameters
     * @return The HTTP response
     */
    suspend fun get(
        path: String,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ): HttpResponse = execute(
        HttpRequest(
            method = HttpMethod.GET,
            path = path,
            headers = headers,
            queryParams = queryParams
        )
    )

    /**
     * Convenience method for making a POST request.
     *
     * @param path The request path
     * @param body The request body
     * @param headers Additional headers
     * @param queryParams Query parameters
     * @return The HTTP response
     */
    suspend fun post(
        path: String,
        body: RequestBody? = null,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ): HttpResponse = execute(
        HttpRequest(
            method = HttpMethod.POST,
            path = path,
            headers = headers,
            queryParams = queryParams,
            body = body
        )
    )

    /**
     * Convenience method for making a PUT request.
     *
     * @param path The request path
     * @param body The request body
     * @param headers Additional headers
     * @param queryParams Query parameters
     * @return The HTTP response
     */
    suspend fun put(
        path: String,
        body: RequestBody? = null,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ): HttpResponse = execute(
        HttpRequest(
            method = HttpMethod.PUT,
            path = path,
            headers = headers,
            queryParams = queryParams,
            body = body
        )
    )

    /**
     * Convenience method for making a DELETE request.
     *
     * @param path The request path
     * @param headers Additional headers
     * @param queryParams Query parameters
     * @return The HTTP response
     */
    suspend fun delete(
        path: String,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ): HttpResponse = execute(
        HttpRequest(
            method = HttpMethod.DELETE,
            path = path,
            headers = headers,
            queryParams = queryParams
        )
    )

    /**
     * Uploads a file using multipart/form-data.
     *
     * @param path The request path
     * @param file The file to upload
     * @param additionalParts Additional form parts to include
     * @param headers Additional headers
     * @return The HTTP response
     */
    suspend fun uploadFile(
        path: String,
        file: Part.File,
        additionalParts: List<Part> = emptyList(),
        headers: Map<String, String> = emptyMap()
    ): HttpResponse = post(
        path = path,
        body = RequestBody.Multipart(listOf(file) + additionalParts),
        headers = headers
    )
}

/**
 * Extension function to create a JSON request body from a serializable object.
 */
inline fun <reified T> T.toJsonBody(): RequestBody.Json {
    val json = kotlinx.serialization.json.Json.encodeToString(
        kotlinx.serialization.serializer<T>(),
        this
    )
    return RequestBody.Json(json)
}

/**
 * Extension function to parse a JSON response body into an object.
 */
inline fun <reified T> HttpResponse.parseJson(): T {
    val json = body.decodeToString()
    return kotlinx.serialization.json.Json.decodeFromString(
        kotlinx.serialization.serializer<T>(),
        json
    )
}

/**
 * Extension function to parse a JSON response body into an [ApiResponse].
 */
inline fun <reified T> HttpResponse.parseApiResponse(): ApiResponse<T> {
    return parseJson<ApiResponse<T>>()
}

/**
 * Extension function to handle common HTTP errors and convert them to [ApiException].
 */
fun HttpResponse.throwIfError() {
    if (statusCode >= 400) {
        val error = try {
            parseJson<ApiError>()
        } catch (e: Exception) {
            ApiError(
                code = statusCode,
                message = "HTTP Error: $statusCode",
                statusCode = statusCode
            )
        }
        throw ApiException(error)
    }
}

/**
 * Platform-specific implementation of [ChatHttpClient].
 * This is an expect class that must be implemented for each platform.
 *
 * @property config The HTTP client configuration
 */
expect class PlatformChatHttpClient(config: HttpClientConfig) : ChatHttpClient {
    /**
     * Closes the HTTP client and releases all resources.
     * This should be called when the client is no longer needed.
     */
    fun close()
} 