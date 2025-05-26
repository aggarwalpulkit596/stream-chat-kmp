package io.getstream.chat.client

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.promise
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestMode
import org.w3c.fetch.RequestRedirect

/**
 * JavaScript implementation of [ChatHttpClient] using Ktor's JS engine.
 * Handles both browser and Node.js environments.
 *
 * @property config The HTTP client configuration
 * @property isDebug Whether the app is running in debug mode
 * @property isBrowser Whether the code is running in a browser environment
 */
actual class PlatformChatHttpClient(
    private val config: HttpClientConfig,
    private val isDebug: Boolean = false,
    private val isBrowser: Boolean = js("typeof window !== 'undefined'") as Boolean
) : ChatHttpClient {

    override val config: HttpClientConfig = config

    private val client: HttpClient by lazy {
        val engine = JsEngine {
            // Configure fetch options
            fetchOptions {
                // Handle CORS in browser environment
                if (isBrowser) {
                    mode = RequestMode.CORS
                    credentials = RequestCredentials.SAME_ORIGIN
                    redirect = RequestRedirect.FOLLOW
                }
            }
        }

        HttpClient(engine) {
            // Configure content negotiation
            install(ContentNegotiation) {
                json()
            }

            // Configure logging
            if (isDebug) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.ALL
                }
            }

            // Configure default request
            install(DefaultRequest) {
                url(config.baseUrl)
                headers {
                    config.defaultHeaders.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }

            // Configure retry
            install(HttpTimeout) {
                connectTimeoutMillis = config.timeout.connect.inWholeMilliseconds
                requestTimeoutMillis = config.timeout.read.inWholeMilliseconds
                socketTimeoutMillis = config.timeout.write.inWholeMilliseconds
            }
        }
    }

    override suspend fun execute(request: HttpRequest): HttpResponse = kotlinx.coroutines.promise {
        try {
            val response = client.request {
                method = when (request.method) {
                    HttpMethod.GET -> HttpMethod.Get
                    HttpMethod.POST -> HttpMethod.Post
                    HttpMethod.PUT -> HttpMethod.Put
                    HttpMethod.DELETE -> HttpMethod.Delete
                    HttpMethod.PATCH -> HttpMethod.Patch
                }
                url {
                    takeFrom(config.baseUrl)
                    appendPathSegments(request.path.trimStart('/'))
                    request.queryParams.forEach { (key, value) ->
                        parameters.append(key, value)
                    }
                }
                headers {
                    request.headers.forEach { (key, value) ->
                        append(key, value)
                    }
                }
                when (val body = request.body) {
                    is RequestBody.Empty -> Unit
                    is RequestBody.Json -> setBody(body.json)
                    is RequestBody.Form -> {
                        contentType(ContentType.Application.FormUrlEncoded)
                        setBody(body.formData.entries.joinToString("&") { (key, value) ->
                            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
                        })
                    }
                    is RequestBody.Multipart -> {
                        contentType(ContentType.MultiPart.FormData)
                        body.parts.forEach { part ->
                            when (part) {
                                is Part.FormField -> {
                                    append(part.name, part.value)
                                }
                                is Part.File -> {
                                    append(part.name, part.content) {
                                        filename = part.filename
                                        contentType = ContentType.parse(part.contentType)
                                    }
                                }
                            }
                        }
                    }
                    null -> Unit
                }
            }

            HttpResponse(
                statusCode = response.status.value,
                headers = response.headers.entries().associate { it.key to it.value },
                body = response.bodyAsText().toByteArray()
            )
        } catch (e: Exception) {
            throw when (e) {
                is io.ktor.client.plugins.ClientRequestException -> HttpException(
                    statusCode = e.response.status.value,
                    message = e.message ?: "Request failed",
                    response = e.response.bodyAsText().toByteArray()
                )
                else -> HttpException(
                    statusCode = 0,
                    message = e.message ?: "Unknown error",
                    response = null
                )
            }
        }
    }.await()

    override fun executeAsFlow(request: HttpRequest): Flow<ByteArray> = flow {
        try {
            val response = client.request {
                // ... same request configuration as execute() ...
            }
            response.bodyAsChannel().toByteArray().let { emit(it) }
        } catch (e: Exception) {
            throw when (e) {
                is io.ktor.client.plugins.ClientRequestException -> HttpException(
                    statusCode = e.response.status.value,
                    message = e.message ?: "Request failed",
                    response = e.response.bodyAsText().toByteArray()
                )
                else -> HttpException(
                    statusCode = 0,
                    message = e.message ?: "Unknown error",
                    response = null
                )
            }
        }
    }

    actual fun close() {
        client.close()
    }
} 