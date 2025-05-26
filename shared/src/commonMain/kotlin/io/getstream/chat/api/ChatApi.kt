package io.getstream.chat.api

import io.getstream.chat.models.*
import kotlinx.coroutines.flow.Flow

/**
 * TODO: Define the main API interface for chat operations:
 * - Channel operations (create, update, delete, query)
 * - Message operations (send, update, delete, query)
 * - User operations (create, update, delete, query)
 * - WebSocket operations (connect, disconnect, subscribe)
 * - Event handling
 */

interface ChatApi {
    // Channel operations
    suspend fun createChannel(channel: Channel): Channel
    suspend fun updateChannel(channel: Channel): Channel
    suspend fun deleteChannel(channelId: String)
    suspend fun queryChannels(filter: Map<String, Any>): List<Channel>
    
    // Message operations
    suspend fun sendMessage(channelId: String, message: Message): Message
    suspend fun updateMessage(message: Message): Message
    suspend fun deleteMessage(messageId: String)
    suspend fun queryMessages(channelId: String, limit: Int = 20): List<Message>
    
    // User operations
    suspend fun createUser(user: User): User
    suspend fun updateUser(user: User): User
    suspend fun deleteUser(userId: String)
    suspend fun queryUsers(filter: Map<String, Any>): List<User>
    
    // WebSocket operations
    suspend fun connect(userId: String)
    suspend fun disconnect()
    suspend fun subscribeToChannel(channelId: String)
    suspend fun unsubscribeFromChannel(channelId: String)
    
    // Event handling
    fun observeChannelEvents(channelId: String): Flow<ChannelEvent>
    fun observeMessageEvents(channelId: String): Flow<MessageEvent>
    fun observeUserEvents(userId: String): Flow<UserEvent>
}

sealed class ChannelEvent {
    data class Updated(val channel: Channel) : ChannelEvent()
    data class Deleted(val channelId: String) : ChannelEvent()
    data class MemberAdded(val channelId: String, val member: Member) : ChannelEvent()
    data class MemberRemoved(val channelId: String, val userId: String) : ChannelEvent()
}

sealed class MessageEvent {
    data class New(val message: Message) : MessageEvent()
    data class Updated(val message: Message) : MessageEvent()
    data class Deleted(val messageId: String) : MessageEvent()
    data class ReactionAdded(val messageId: String, val reaction: Reaction) : MessageEvent()
    data class ReactionRemoved(val messageId: String, val reaction: Reaction) : MessageEvent()
}

sealed class UserEvent {
    data class Updated(val user: User) : UserEvent()
    data class Deleted(val userId: String) : UserEvent()
    data class Online(val userId: String) : UserEvent()
    data class Offline(val userId: String) : UserEvent()
} 