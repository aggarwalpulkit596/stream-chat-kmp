package io.getstream.chat.models

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonElement
import io.getstream.chat.serialization.InstantSerializer
import io.getstream.chat.serialization.MapSerializer

/**
 * Represents a generic API response from the Stream Chat API.
 * This is a sealed class that can be either a [Success] or [Error].
 *
 * @param T The type of data contained in the response.
 */
@Serializable
sealed class ApiResponse<out T> {
    /**
     * Represents a successful API response.
     *
     * @property data The response data.
     * @property duration The duration of the API call in milliseconds.
     */
    @Serializable
    data class Success<T>(
        val data: T,
        val duration: String
    ) : ApiResponse<T>()

    /**
     * Represents an error API response.
     *
     * @property error The error details.
     * @property duration The duration of the API call in milliseconds.
     */
    @Serializable
    data class Error(
        val error: ApiError,
        val duration: String
    ) : ApiResponse<Nothing>()
}

/**
 * Represents an error from the Stream Chat API.
 *
 * @property code The error code.
 * @property message The error message.
 * @property statusCode The HTTP status code.
 * @property details Additional error details.
 */
@Serializable
data class ApiError(
    val code: Int,
    val message: String,
    val statusCode: Int,
    val details: Map<String, JsonElement> = emptyMap()
)

/**
 * Represents a paginated response from the Stream Chat API.
 *
 * @property next The cursor for the next page.
 * @property previous The cursor for the previous page.
 */
@Serializable
data class Pagination(
    val next: String? = null,
    val previous: String? = null
)

/**
 * Represents a response containing a list of items with pagination.
 *
 * @property items The list of items.
 * @property pagination The pagination information.
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val pagination: Pagination
)

/**
 * Represents a channel response from the Stream Chat API.
 *
 * @property channel The channel data.
 * @property members The list of channel members.
 * @property messages The list of messages in the channel.
 * @property pinnedMessages The list of pinned messages.
 * @property watcherCount The number of users watching the channel.
 * @property watchers The list of users watching the channel.
 * @property read The list of read states.
 * @property typing The list of typing states.
 */
@Serializable
data class ChannelResponse(
    val channel: Channel,
    val members: List<Member> = emptyList(),
    val messages: List<Message> = emptyList(),
    val pinnedMessages: List<Message> = emptyList(),
    val watcherCount: Int = 0,
    val watchers: List<User> = emptyList(),
    val read: List<Read> = emptyList(),
    val typing: Map<String, TypingState> = emptyMap()
)

/**
 * Represents a message response from the Stream Chat API.
 *
 * @property message The message data.
 * @property parentMessage The parent message if this is a thread reply.
 * @property threadParticipants The list of users participating in the thread.
 */
@Serializable
data class MessageResponse(
    val message: Message,
    val parentMessage: Message? = null,
    val threadParticipants: List<User> = emptyList()
)

/**
 * Represents a user response from the Stream Chat API.
 *
 * @property user The user data.
 * @property devices The list of user devices.
 * @property presence The user's presence information.
 */
@Serializable
data class UserResponse(
    val user: User,
    val devices: List<Device> = emptyList(),
    val presence: Presence? = null
)

/**
 * Represents a presence state for a user.
 *
 * @property lastActive When the user was last active.
 * @property status The user's status (online, offline, etc.).
 */
@Serializable
data class Presence(
    @Serializable(with = InstantSerializer::class)
    val lastActive: Instant? = null,
    val status: String = "offline"
)

/**
 * Represents a read state for a channel.
 *
 * @property user The user who read the channel.
 * @property lastRead The last read message ID.
 * @property unreadMessages The number of unread messages.
 * @property lastReadAt When the channel was last read.
 */
@Serializable
data class Read(
    val user: User,
    val lastRead: String? = null,
    val unreadMessages: Int = 0,
    @Serializable(with = InstantSerializer::class)
    val lastReadAt: Instant? = null
)

/**
 * Represents a typing state for a channel.
 *
 * @property user The user who is typing.
 * @property startedAt When the user started typing.
 */
@Serializable
data class TypingState(
    val user: User,
    @Serializable(with = InstantSerializer::class)
    val startedAt: Instant
)

/**
 * Represents an event from the Stream Chat API.
 *
 * @property type The type of event.
 * @property cid The channel ID where the event occurred.
 * @property channel The channel where the event occurred.
 * @property message The message related to the event.
 * @property user The user who triggered the event.
 * @property createdAt When the event occurred.
 * @property extraData Additional event data.
 */
@Serializable
data class Event(
    val type: String,
    val cid: String? = null,
    val channel: Channel? = null,
    val message: Message? = null,
    val user: User? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = MapSerializer::class)
    val extraData: Map<String, JsonElement> = emptyMap()
)

/**
 * Represents an event response from the Stream Chat API.
 *
 * @property event The event data.
 * @property connectionId The connection ID.
 * @property createdAt When the event was received.
 */
@Serializable
data class EventResponse(
    val event: Event,
    val connectionId: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null
)

/**
 * Extension function to safely get the data from an [ApiResponse].
 * Returns null if the response is an error.
 */
fun <T> ApiResponse<T>.getOrNull(): T? = when (this) {
    is ApiResponse.Success -> data
    is ApiResponse.Error -> null
}

/**
 * Extension function to get the data from an [ApiResponse] or throw an exception.
 * Throws [ApiException] if the response is an error.
 */
fun <T> ApiResponse<T>.getOrThrow(): T = when (this) {
    is ApiResponse.Success -> data
    is ApiResponse.Error -> throw ApiException(error)
}

/**
 * Extension function to map the data of an [ApiResponse] if it's successful.
 */
inline fun <T, R> ApiResponse<T>.map(transform: (T) -> R): ApiResponse<R> = when (this) {
    is ApiResponse.Success -> ApiResponse.Success(transform(data), duration)
    is ApiResponse.Error -> ApiResponse.Error(error, duration)
}

/**
 * Extension function to handle both success and error cases of an [ApiResponse].
 */
inline fun <T> ApiResponse<T>.fold(
    onSuccess: (T) -> Unit,
    onError: (ApiError) -> Unit
) = when (this) {
    is ApiResponse.Success -> onSuccess(data)
    is ApiResponse.Error -> onError(error)
}

/**
 * Exception thrown when an API response contains an error.
 *
 * @property error The API error details.
 */
class ApiException(val error: ApiError) : Exception("API Error: ${error.message} (code: ${error.code})")

/**
 * Common error codes for the Stream Chat API.
 */
object ErrorCodes {
    const val TOKEN_EXPIRED = 40
    const val INVALID_TOKEN = 41
    const val RATE_LIMIT_EXCEEDED = 9
    const val CHANNEL_NOT_FOUND = 16
    const val MESSAGE_NOT_FOUND = 17
    const val USER_NOT_FOUND = 18
    const val PERMISSION_DENIED = 19
    const val INVALID_REQUEST = 4
    const val INTERNAL_ERROR = 0
}

/**
 * Extension function to check if an [ApiError] is due to an expired token.
 */
val ApiError.isTokenExpired: Boolean get() = code == ErrorCodes.TOKEN_EXPIRED

/**
 * Extension function to check if an [ApiError] is due to an invalid token.
 */
val ApiError.isInvalidToken: Boolean get() = code == ErrorCodes.INVALID_TOKEN

/**
 * Extension function to check if an [ApiError] is due to rate limiting.
 */
val ApiError.isRateLimited: Boolean get() = code == ErrorCodes.RATE_LIMIT_EXCEEDED

/**
 * Extension function to check if an [ApiError] is due to a not found error.
 */
val ApiError.isNotFound: Boolean get() = code in listOf(
    ErrorCodes.CHANNEL_NOT_FOUND,
    ErrorCodes.MESSAGE_NOT_FOUND,
    ErrorCodes.USER_NOT_FOUND
)

/**
 * Extension function to check if an [ApiError] is due to permission issues.
 */
val ApiError.isPermissionDenied: Boolean get() = code == ErrorCodes.PERMISSION_DENIED

/**
 * Extension function to check if an [ApiError] is due to an invalid request.
 */
val ApiError.isInvalidRequest: Boolean get() = code == ErrorCodes.INVALID_REQUEST

/**
 * Extension function to check if an [ApiError] is due to an internal error.
 */
val ApiError.isInternalError: Boolean get() = code == ErrorCodes.INTERNAL_ERROR
