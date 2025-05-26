package io.getstream.chat.client

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * Android implementation of ChatHttpClient using Ktor with OkHttp engine.
 */
actual class PlatformChatHttpClient actual constructor(
    override val config: HttpClientConfig
) : ChatHttpClient {
    
    private val ktorClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        
        install(Logging) {
            level = LogLevel.INFO
        }
        
        install(HttpTimeout) {
            connectTimeoutMillis = config.timeout.connect.inWholeMilliseconds
            requestTimeoutMillis = config.timeout.read.inWholeMilliseconds
            socketTimeoutMillis = config.timeout.write.inWholeMilliseconds
        }
        
        defaultRequest {
            url(config.baseUrl)
            config.defaultHeaders.forEach { (key, value) ->
                header(key, value)
            }
        }
    }

    override suspend fun execute(request: HttpRequest): HttpResponse {
        val response = ktorClient.request {
            method = when (request.method) {
                HttpMethod.GET -> io.ktor.http.HttpMethod.Get
                HttpMethod.POST -> io.ktor.http.HttpMethod.Post
                HttpMethod.PUT -> io.ktor.http.HttpMethod.Put
                HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
                HttpMethod.PATCH -> io.ktor.http.HttpMethod.Patch
            }
            
            url {
                appendPathSegments(request.path.removePrefix("/"))
                request.queryParams.forEach { (key, value) ->
                    parameters.append(key, value)
                }
            }
            
            request.headers.forEach { (key, value) ->
                header(key, value)
            }
            
            when (val body = request.body) {
                is RequestBody.Json -> {
                    contentType(ContentType.Application.Json)
                    setBody(body.json)
                }
                is RequestBody.Form -> {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(body.formData.entries.joinToString("&") { "${it.key}=${it.value}" })
                }
                is RequestBody.Multipart -> {
                    // Handle multipart requests
                    setBody(MultiPartFormDataContent(
                        formData {
                            body.parts.forEach { part ->
                                when (part) {
                                    is Part.FormField -> {
                                        append(part.name, part.value)
                                    }
                                    is Part.File -> {
                                        append(
                                            part.name,
                                            part.content,
                                            Headers.build {
                                                append(HttpHeaders.ContentType, part.contentType)
                                                append(HttpHeaders.ContentDisposition, "filename=\"${part.filename}\"")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ))
                }
                RequestBody.Empty -> {
                    // No body
                }
            }
        }
        
        return HttpResponse(
            statusCode = response.status.value,
            headers = response.headers.toMap().mapValues { it.value.first() },
            body = response.readBytes()
        )
    }

    override fun executeAsFlow(request: HttpRequest): Flow<ByteArray> = flow {
        val response = execute(request)
        emit(response.body)
    }

    actual fun close() {
        ktorClient.close()
    }
} 