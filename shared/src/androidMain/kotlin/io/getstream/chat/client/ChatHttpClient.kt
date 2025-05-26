package io.getstream.chat.client

import android.content.Context
import android.os.Build
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Android implementation of [ChatHttpClient] using Ktor's OkHttp engine.
 *
 * @property config The HTTP client configuration
 * @property context The Android application context
 * @property isDebug Whether the app is running in debug mode
 */
actual class PlatformChatHttpClient(
    private val config: HttpClientConfig,
    private val context: Context,
    private val isDebug: Boolean = false
) : ChatHttpClient {

    override val config: HttpClientConfig = config

    private val client: HttpClient by lazy {
        val engine = OkHttpEngine {
            config {
                val okHttpClient = OkHttpClient.Builder().apply {
                    // Configure timeouts
                    connectTimeout(config.timeout.connect)
                    readTimeout(config.timeout.read)
                    writeTimeout(config.timeout.write)

                    // Configure connection pool
                    connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))

                    // Add logging for debug builds
                    if (isDebug) {
                        addInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        })
                    }

                    // Handle network security config
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val networkSecurityConfig = context.applicationContext.resources
                            .getIdentifier("network_security_config", "xml", context.packageName)
                        if (networkSecurityConfig != 0) {
                            // Network security config is present, let the system handle it
                        } else {
                            // No network security config, use default
                            followSslRedirects(true)
                        }
                    }

                    // Add default headers
                    addInterceptor { chain ->
                        val request = chain.request().newBuilder().apply {
                            config.defaultHeaders.forEach { (key, value) ->
                                addHeader(key, value)
                            }
                        }.build()
                        chain.proceed(request)
                    }
                }.build()

                preconfigured = okHttpClient
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
                install(ResponseObserver) {
                    onResponse { response ->
                        println("HTTP status: ${response.status.value}")
                    }
                }
            }

            // Configure default request
            install(DefaultRequest) {
                url(config.baseUrl)
            }

            // Configure retry
            install(HttpTimeout) {
                connectTimeoutMillis = config.timeout.connect.inWholeMilliseconds
                requestTimeoutMillis = config.timeout.read.inWholeMilliseconds
                socketTimeoutMillis = config.timeout.write.inWholeMilliseconds
            }
        }
    }

    override suspend fun execute(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
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
    }

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
    }.flowOn(Dispatchers.IO)

    actual fun close() {
        client.close()
    }
} 