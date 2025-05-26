package io.getstream.chat.client

import io.getstream.chat.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.encodeToString
import kotlin.time.Duration

/**
 * Client for interacting with the Stream Chat REST API.
 * Handles authentication, request signing, and implements core API endpoints.
 *
 * @property httpClient The HTTP client to use for making requests
 * @property apiKey The Stream Chat API key
 * @property token The JWT token for authentication
 * @property userId The ID of the authenticated user
 * @property logger Optional logger for API operations
 */
class ChatApiClient(
    private val httpClient: ChatHttpClient,
    private val apiKey: String,
    private var token: String? = null,
    private var userId: String? = null,
    private val logger: ChatLogger? = null
) {
    companion object {
        private const val API_VERSION = "v1"
        private const val DEFAULT_LIMIT = 30
        private const val RATE_LIMIT_HEADER = "X-RateLimit-Remaining"
        private const val RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset"
    }

    /**
     * Logger interface for API operations.
     */
    interface ChatLogger {
        fun debug(message: String, vararg args: Any?)
        fun info(message: String, vararg args: Any?)
        fun warn(message: String, vararg args: Any?)
        fun error(message: String, vararg args: Any?)
    }

    /**
     * Configuration for channel query parameters.
     */
    data class ChannelQueryConfig(
        val filter: Map<String, String> = emptyMap(),
        val sort: List<String> = emptyList(),
        val limit: Int = DEFAULT_LIMIT,
        val offset: Int = 0,
        val messageLimit: Int = DEFAULT_LIMIT,
        val presence: Boolean = false,
        val state: Boolean = true,
        val watch: Boolean = false,
        val members: Boolean = true,
        val watchers: Boolean = false
    )

    /**
     * Configuration for message query parameters.
     */
    data class MessageQueryConfig(
        val limit: Int = DEFAULT_LIMIT,
        val offset: Int = 0,
        val idGt: String? = null,
        val idGte: String? = null,
        val idLt: String? = null,
        val idLte: String? = null,
        val createdAtAfter: Instant? = null,
        val createdAtAfterOrEqual: Instant? = null,
        val createdAtBefore: Instant? = null,
        val createdAtBeforeOrEqual: Instant? = null,
        val includeThreadParticipants: Boolean = false
    )

    /**
     * Rate limit information from API responses.
     */
    data class RateLimitInfo(
        val remaining: Int,
        val resetAt: Instant
    )

    private var lastRateLimitInfo: RateLimitInfo? = null

    /**
     * Authenticates a user with the Stream Chat API.
     *
     * @param userId The ID of the user to authenticate
     * @param token The JWT token for the user
     * @return The user information
     */
    suspend fun connectUser(userId: String, token: String): User {
        this.userId = userId
        this.token = token
        return getUserInfo(userId)
    }

    /**
     * Gets a list of channels based on the provided query configuration.
     *
     * @param config The channel query configuration
     * @return A paginated response of channels
     */
    suspend fun getChannels(config: ChannelQueryConfig): PaginatedResponse<Channel> {
        val queryParams = buildMap {
            put("api_key", apiKey)
            put("limit", config.limit.toString())
            put("offset", config.offset.toString())
            put("message_limit", config.messageLimit.toString())
            put("presence", config.presence.toString())
            put("state", config.state.toString())
            put("watch", config.watch.toString())
            put("members", config.members.toString())
            put("watchers", config.watchers.toString())
        }

        val httpResponse = httpClient.get(
            path = "/channels",
            queryParams = queryParams
        )
        
        updateRateLimitInfo(httpResponse.headers)
        val response = httpResponse.parseApiResponse<PaginatedResponse<Channel>>()
        return response.getOrThrow()
    }

    /**
     * Sends a message to a specific channel.
     *
     * @param channelType The type of the channel
     * @param channelId The ID of the channel
     * @param message The message to send
     * @return The message response
     */
    suspend fun sendMessage(
        channelType: String,
        channelId: String,
        message: Message
    ): MessageResponse {
        val httpResponse = httpClient.post(
            path = "/channels/$channelType/$channelId/message",
            body = message.toJsonBody()
        )

        updateRateLimitInfo(httpResponse.headers)
        val response = httpResponse.parseApiResponse<MessageResponse>()
        return response.getOrThrow()
    }

    /**
     * Gets messages from a specific channel.
     *
     * @param channelType The type of the channel
     * @param channelId The ID of the channel
     * @param config The message query configuration
     * @return A paginated response of messages
     */
    suspend fun getMessages(
        channelType: String,
        channelId: String,
        config: MessageQueryConfig
    ): PaginatedResponse<Message> {
        val queryParams = buildMap {
            put("api_key", apiKey)
            put("limit", config.limit.toString())
            put("offset", config.offset.toString())
            config.idGt?.let { put("id_gt", it) }
            config.idGte?.let { put("id_gte", it) }
            config.idLt?.let { put("id_lt", it) }
            config.idLte?.let { put("id_lte", it) }
            config.createdAtAfter?.let { put("created_at_after", it.toString()) }
            config.createdAtAfterOrEqual?.let { put("created_at_after_or_equal", it.toString()) }
            config.createdAtBefore?.let { put("created_at_before", it.toString()) }
            config.createdAtBeforeOrEqual?.let { put("created_at_before_or_equal", it.toString()) }
            put("include_thread_participants", config.includeThreadParticipants.toString())
        }

        val httpResponse = httpClient.get(
            path = "/channels/$channelType/$channelId/messages",
            queryParams = queryParams
        )

        updateRateLimitInfo(httpResponse.headers)
        val response = httpResponse.parseApiResponse<PaginatedResponse<Message>>()
        return response.getOrThrow()
    }

    /**
     * Updates an existing message.
     *
     * @param messageId The ID of the message to update
     * @param update The message update data
     * @return The updated message
     */
    suspend fun updateMessage(
        messageId: String,
        update: Message
    ): Message {
        val httpResponse = httpClient.put(
            path = "/messages/$messageId",
            body = update.toJsonBody()
        )

        updateRateLimitInfo(httpResponse.headers)
        val response = httpResponse.parseApiResponse<MessageResponse>()
        return response.getOrThrow().message
    }

    /**
     * Deletes a message.
     *
     * @param messageId The ID of the message to delete
     * @param hardDelete Whether to perform a hard delete
     * @return The deleted message
     */
    suspend fun deleteMessage(
        messageId: String,
        hardDelete: Boolean = false
    ): Message {
        val queryParams = mapOf(
            "hard" to hardDelete.toString()
        )

        val httpResponse = httpClient.delete(
            path = "/messages/$messageId",
            queryParams = queryParams
        )

        updateRateLimitInfo(httpResponse.headers)
        val response = httpResponse.parseApiResponse<MessageResponse>()
        return response.getOrThrow().message
    }

    /**
     * Gets information about a user.
     *
     * @param userId The ID of the user
     * @return The user information
     */
    suspend fun getUserInfo(userId: String): User {
        val httpResponse = httpClient.get(
            path = "/users/$userId"
        )

        updateRateLimitInfo(httpResponse.headers)
        val response = httpResponse.parseApiResponse<UserResponse>()
        return response.getOrThrow().user
    }

    /**
     * Updates a user's information.
     *
     * @param userId The ID of the user to update
     * @param update The user update data
     * @return The updated user
     */
    suspend fun updateUser(userId: String, update: User): User {
        val httpResponse = httpClient.put(
            path = "/users/$userId",
            body = update.toJsonBody()
        )

        updateRateLimitInfo(httpResponse.headers)
        val response = httpResponse.parseApiResponse<UserResponse>()
        return response.getOrThrow().user
    }

    /**
     * Creates a new channel.
     *
     * @param channelType The type of the channel
     * @param channelId The ID of the channel
     * @param members The list of member user IDs
     * @param extraData Additional channel data
     * @return The created channel
     */
    suspend fun createChannel(
        channelType: String,
        channelId: String,
        members: List<String>,
        extraData: Map<String, String> = emptyMap()
    ): Channel {
        val data = buildJsonObject {
            put("members", Json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer()), members))
            put("created_by_id", JsonPrimitive(userId ?: throw IllegalStateException("User not connected")))
            if (extraData.isNotEmpty()) {
                extraData.forEach { (key, value) ->
                    put(key, JsonPrimitive(value))
                }
            }
        }

        val httpResponse = httpClient.post(
            path = "/channels/$channelType",
            queryParams = mapOf("id" to channelId),
            body = RequestBody.Json(data.toString())
        )

        updateRateLimitInfo(httpResponse.headers)
        val response = httpResponse.parseApiResponse<ChannelResponse>()
        return response.getOrThrow().channel
    }

    /**
     * Uploads a file to a channel.
     *
     * @param channelType The type of the channel
     * @param channelId The ID of the channel
     * @param file The file to upload
     * @return The uploaded file information
     */
    suspend fun uploadFile(
        channelType: String,
        channelId: String,
        file: Part.File
    ): Attachment {
        val httpResponse = httpClient.uploadFile(
            path = "/channels/$channelType/$channelId/file",
            file = file
        )

        updateRateLimitInfo(httpResponse.headers)
        val response = httpResponse.parseApiResponse<MessageResponse>()
        return response.getOrThrow().message.attachments.firstOrNull()
            ?: throw IllegalStateException("No attachment in response")
    }

    /**
     * Gets the current rate limit information.
     */
    fun getRateLimitInfo(): RateLimitInfo? = lastRateLimitInfo

    /**
     * Checks if the current rate limit is close to being exceeded.
     *
     * @param threshold The threshold at which to consider the rate limit close to being exceeded
     * @return Whether the rate limit is close to being exceeded
     */
    fun isRateLimitCloseToExceeded(threshold: Int = 10): Boolean {
        val info = lastRateLimitInfo ?: return false
        return info.remaining <= threshold
    }

    private fun updateRateLimitInfo(headers: Map<String, String>) {
        val remaining = headers[RATE_LIMIT_HEADER]?.toIntOrNull()
        val resetAt = headers[RATE_LIMIT_RESET_HEADER]?.toLongOrNull()?.let { 
            Instant.fromEpochSeconds(it)
        }

        if (remaining != null && resetAt != null) {
            lastRateLimitInfo = RateLimitInfo(remaining, resetAt)
            logger?.debug("Rate limit: $remaining remaining, resets at $resetAt")
        }
    }
} 