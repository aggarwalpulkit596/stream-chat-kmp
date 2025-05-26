package io.getstream.chat.models

import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonElement
import io.getstream.chat.serialization.InstantSerializer
import io.getstream.chat.serialization.MapSerializer

/**
 * TODO: Define core data models for the chat SDK:
 * - User
 * - Channel
 * - Message
 * - Reaction
 * - Attachment
 * - Member
 * - ChannelType
 * - MessageType
 * - etc.
 */

/**
 * Represents a person who uses a chat and can perform chat operations like viewing channels or sending messages.
 *
 * @property id The unique id of the user. This field is required.
 * @property role Determines the set of user permissions.
 * @property name User's name.
 * @property image User's image URL.
 * @property invisible Determines if the user should share its online status. Can only be changed while connecting the user.
 * @property privacySettings The privacy settings for the user.
 * @property language User's preferred language.
 * @property banned Whether a user is banned or not.
 * @property devices The list of devices for the current user.
 * @property online Whether a user is online or not.
 * @property createdAt Date/time of creation.
 * @property updatedAt Date/time of the last update.
 * @property lastActive Date of last activity.
 * @property totalUnreadCount The total unread messages count for the current user.
 * @property unreadChannels The total unread channels count for the current user.
 * @property unreadThreads The total number of unread threads for the current user.
 * @property mutes A list of users muted by the current user.
 * @property teams List of teams user is a part of.
 * @property teamsRole The roles of the user in the teams they are part of. Example: `["teamId": "role"]`.
 * @property channelMutes A list of channels muted by the current user.
 * @property blockedUserIds A list of user ids blocked by the current user.
 * @property extraData A map of custom fields for the user.
 * @property deactivatedAt Date/time of deactivation.
 */
@Serializable
data class User(
    val id: String = "",
    val role: String = "",
    val name: String = "",
    val image: String = "",
    val invisible: Boolean? = null,
    val privacySettings: PrivacySettings? = null,
    val language: String = "",
    val banned: Boolean? = null,
    val devices: List<Device> = emptyList(),
    val online: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val lastActive: Instant? = null,
    val totalUnreadCount: Int = 0,
    val unreadChannels: Int = 0,
    val unreadThreads: Int = 0,
    val mutes: List<Mute> = emptyList(),
    val teams: List<String> = emptyList(),
    val teamsRole: Map<String, String> = emptyMap(),
    val channelMutes: List<ChannelMute> = emptyList(),
    val blockedUserIds: List<String> = emptyList(),
    @Serializable(with = MapSerializer::class)
    val extraData: Map<String, JsonElement> = emptyMap(),
    @Serializable(with = InstantSerializer::class)
    val deactivatedAt: Instant? = null
) {
    /**
     * Determines if the user is banned or not.
     */
    val isBanned: Boolean get() = banned == true

    /**
     * Determines if the user should share its online status.
     */
    val isInvisible: Boolean get() = invisible == true

    /**
     * Determines if the user has typing indicators enabled.
     */
    val isTypingIndicatorsEnabled: Boolean get() = privacySettings?.typingIndicators?.enabled ?: true

    /**
     * Determines if the user has read receipts enabled.
     */
    val isReadReceiptsEnabled: Boolean get() = privacySettings?.readReceipts?.enabled ?: true

    /**
     * Determines if the user is deleted.
     */
    val isDeleted: Boolean get() = deactivatedAt != null
}

/**
 * Represents a device associated with a user.
 *
 * @property id The unique identifier of the device.
 * @property pushProvider The push notification provider for this device.
 * @property createdAt When the device was created.
 */
@Serializable
data class Device(
    val id: String,
    val pushProvider: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

/**
 * Represents a mute relationship between users.
 *
 * @property user The user who is muted.
 * @property target The user who muted the user.
 * @property createdAt When the mute was created.
 */
@Serializable
data class Mute(
    val user: User,
    val target: User,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

/**
 * Represents a channel mute.
 *
 * @property channel The channel that is muted.
 * @property user The user who muted the channel.
 * @property createdAt When the channel was muted.
 * @property expiresAt When the mute expires, if applicable.
 */
@Serializable
data class ChannelMute(
    val channel: Channel,
    val user: User,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val expiresAt: Instant? = null
)

/**
 * Represents privacy settings for a user.
 *
 * @property typingIndicators Settings for typing indicators.
 * @property readReceipts Settings for read receipts.
 */
@Serializable
data class PrivacySettings(
    val typingIndicators: TypingIndicatorSettings = TypingIndicatorSettings(),
    val readReceipts: ReadReceiptSettings = ReadReceiptSettings()
)

/**
 * Settings for typing indicators.
 *
 * @property enabled Whether typing indicators are enabled.
 */
@Serializable
data class TypingIndicatorSettings(
    val enabled: Boolean = true
)

/**
 * Settings for read receipts.
 *
 * @property enabled Whether read receipts are enabled.
 */
@Serializable
data class ReadReceiptSettings(
    val enabled: Boolean = true
)

/**
 * Represents a chat channel where users can exchange messages.
 *
 * @property id The unique identifier of the channel.
 * @property type The type of the channel (e.g., "messaging", "livestream").
 * @property cid The channel ID in the format "type:id".
 * @property name The name of the channel.
 * @property image The image URL of the channel.
 * @property members The list of channel members.
 * @property memberCount The total number of members in the channel.
 * @property watcherCount The number of users currently watching the channel.
 * @property watchers The list of users currently watching the channel.
 * @property config The channel configuration.
 * @property createdBy The user who created the channel.
 * @property frozen Whether the channel is frozen (no new messages allowed).
 * @property disabled Whether the channel is disabled.
 * @property hidden Whether the channel is hidden from the channel list.
 * @property muted Whether the channel is muted for the current user.
 * @property truncatedAt When the channel was truncated (messages before this date were deleted).
 * @property truncatedBy The user who truncated the channel.
 * @property lastMessageAt When the last message was sent in the channel.
 * @property lastMessage The last message sent in the channel.
 * @property createdAt When the channel was created.
 * @property updatedAt When the channel was last updated.
 * @property deletedAt When the channel was deleted.
 * @property team The team the channel belongs to.
 * @property cooldown The cooldown period in seconds between messages.
 * @property extraData Additional custom data for the channel.
 */
@Serializable
data class Channel(
    val id: String,
    val type: String,
    val cid: String = "$type:$id",
    val name: String? = null,
    val image: String? = null,
    val members: List<Member> = emptyList(),
    val memberCount: Int = 0,
    val watcherCount: Int = 0,
    val watchers: List<User> = emptyList(),
    val config: ChannelConfig = ChannelConfig(),
    val createdBy: User? = null,
    val frozen: Boolean = false,
    val disabled: Boolean = false,
    val hidden: Boolean = false,
    val muted: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val truncatedAt: Instant? = null,
    val truncatedBy: User? = null,
    @Serializable(with = InstantSerializer::class)
    val lastMessageAt: Instant? = null,
    val lastMessage: Message? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val deletedAt: Instant? = null,
    val team: String? = null,
    val cooldown: Int = 0,
    @Serializable(with = MapSerializer::class)
    val extraData: Map<String, JsonElement> = emptyMap()
) {
    /**
     * Determines if the channel is active (not deleted or disabled).
     */
    val isActive: Boolean get() = !disabled && deletedAt == null

    /**
     * Determines if the channel is writable (not frozen or disabled).
     */
    val isWritable: Boolean get() = isActive && !frozen

    /**
     * Gets the member count excluding deleted users.
     */
    val activeMemberCount: Int get() = members.count { !it.user.isDeleted }
}

/**
 * Represents a member of a channel with their role and permissions.
 *
 * @property user The user who is a member of the channel.
 * @property role The role of the member in the channel.
 * @property channelRole The role of the member in the channel (deprecated, use role instead).
 * @property isChannelMember Whether the user is a member of the channel.
 * @property isModerator Whether the user is a moderator of the channel.
 * @property isOwner Whether the user is the owner of the channel.
 * @property isAdmin Whether the user is an admin of the channel.
 * @property isBanned Whether the user is banned from the channel.
 * @property isShadowBanned Whether the user is shadow banned in the channel.
 * @property bannedAt When the user was banned from the channel.
 * @property bannedBy The user who banned this member.
 * @property banExpiresAt When the ban expires.
 * @property inviteAcceptedAt When the user accepted the channel invite.
 * @property inviteRejectedAt When the user rejected the channel invite.
 * @property invitedAt When the user was invited to the channel.
 * @property invitedBy The user who invited this member.
 * @property createdAt When the member was added to the channel.
 * @property updatedAt When the member's role was last updated.
 * @property extraData Additional custom data for the member.
 */
@Serializable
data class Member(
    val user: User,
    val role: String = "member",
    @Deprecated("Use role instead")
    val channelRole: String = role,
    val isChannelMember: Boolean = true,
    val isModerator: Boolean = false,
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false,
    val isBanned: Boolean = false,
    val isShadowBanned: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val bannedAt: Instant? = null,
    val bannedBy: User? = null,
    @Serializable(with = InstantSerializer::class)
    val banExpiresAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val inviteAcceptedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val inviteRejectedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val invitedAt: Instant? = null,
    val invitedBy: User? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
    @Serializable(with = MapSerializer::class)
    val extraData: Map<String, JsonElement> = emptyMap()
) {
    /**
     * Determines if the member has moderator permissions.
     */
    val hasModeratorPermissions: Boolean get() = isModerator || isOwner || isAdmin

    /**
     * Determines if the member can moderate the channel.
     */
    val canModerate: Boolean get() = hasModeratorPermissions && !isBanned && !isShadowBanned

    /**
     * Determines if the member is currently banned.
     */
    val isCurrentlyBanned: Boolean get() = isBanned && (banExpiresAt == null || banExpiresAt > Clock.System.now())
}

/**
 * Represents the configuration and capabilities of a channel.
 *
 * @property name The name of the channel.
 * @property typingEvents Whether typing events are enabled.
 * @property readEvents Whether read events are enabled.
 * @property connectEvents Whether connect events are enabled.
 * @property search Whether search is enabled.
 * @property reactions Whether reactions are enabled.
 * @property replies Whether replies are enabled.
 * @property mutes Whether mutes are enabled.
 * @property uploads Whether file uploads are enabled.
 * @property urlEnrichment Whether URL enrichment is enabled.
 * @property customEvents Whether custom events are enabled.
 * @property pushNotifications Whether push notifications are enabled.
 * @property messageRetention The message retention period in days.
 * @property maxMessageLength The maximum length of a message.
 * @property automod The automod settings.
 * @property commands The list of available commands.
 * @property createdAt When the configuration was created.
 * @property updatedAt When the configuration was last updated.
 * @property extraData Additional custom data for the configuration.
 */
@Serializable
data class ChannelConfig(
    val name: String = "",
    val typingEvents: Boolean = true,
    val readEvents: Boolean = true,
    val connectEvents: Boolean = true,
    val search: Boolean = true,
    val reactions: Boolean = true,
    val replies: Boolean = true,
    val mutes: Boolean = true,
    val uploads: Boolean = true,
    val urlEnrichment: Boolean = true,
    val customEvents: Boolean = true,
    val pushNotifications: Boolean = true,
    val messageRetention: String = "infinite",
    val maxMessageLength: Int = 5000,
    val automod: AutomodConfig = AutomodConfig(),
    val commands: List<Command> = emptyList(),
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
    @Serializable(with = MapSerializer::class)
    val extraData: Map<String, JsonElement> = emptyMap()
)

/**
 * Represents the automod configuration for a channel.
 *
 * @property behavior The automod behavior (e.g., "flag", "block").
 * @property thresholds The thresholds for different types of content.
 * @property actions The actions to take for different types of content.
 */
@Serializable
data class AutomodConfig(
    val behavior: String = "flag",
    val thresholds: Map<String, Int> = emptyMap(),
    val actions: Map<String, String> = emptyMap()
)

/**
 * Represents a command that can be used in a channel.
 *
 * @property name The name of the command.
 * @property description The description of the command.
 * @property args The arguments the command accepts.
 * @property set The set the command belongs to.
 */
@Serializable
data class Command(
    val name: String,
    val description: String = "",
    val args: String = "",
    val set: String = "default"
)

/**
 * Represents the capabilities of a channel.
 *
 * @property canCreate Whether the user can create messages.
 * @property canRead Whether the user can read messages.
 * @property canUpdate Whether the user can update messages.
 * @property canDelete Whether the user can delete messages.
 * @property canJoin Whether the user can join the channel.
 * @property canLeave Whether the user can leave the channel.
 * @property canAddMembers Whether the user can add members.
 * @property canRemoveMembers Whether the user can remove members.
 * @property canUpdateChannel Whether the user can update the channel.
 * @property canDeleteChannel Whether the user can delete the channel.
 * @property canMuteChannel Whether the user can mute the channel.
 * @property canUnmuteChannel Whether the user can unmute the channel.
 * @property canPinMessages Whether the user can pin messages.
 * @property canUnpinMessages Whether the user can unpin messages.
 * @property canUploadAttachment Whether the user can upload attachments.
 * @property canUseCommands Whether the user can use commands.
 * @property canUseReactions Whether the user can use reactions.
 * @property canUseThreads Whether the user can use threads.
 * @property canUseTypingIndicators Whether the user can use typing indicators.
 * @property canUseReadReceipts Whether the user can use read receipts.
 */
@Serializable
data class ChannelCapabilities(
    val canCreate: Boolean = false,
    val canRead: Boolean = false,
    val canUpdate: Boolean = false,
    val canDelete: Boolean = false,
    val canJoin: Boolean = false,
    val canLeave: Boolean = false,
    val canAddMembers: Boolean = false,
    val canRemoveMembers: Boolean = false,
    val canUpdateChannel: Boolean = false,
    val canDeleteChannel: Boolean = false,
    val canMuteChannel: Boolean = false,
    val canUnmuteChannel: Boolean = false,
    val canPinMessages: Boolean = false,
    val canUnpinMessages: Boolean = false,
    val canUploadAttachment: Boolean = false,
    val canUseCommands: Boolean = false,
    val canUseReactions: Boolean = false,
    val canUseThreads: Boolean = false,
    val canUseTypingIndicators: Boolean = false,
    val canUseReadReceipts: Boolean = false
)

/**
 * Represents the state of a channel.
 *
 * @property isUpToDate Whether the channel state is up to date.
 * @property isInitialized Whether the channel has been initialized.
 * @property isWatching Whether the user is watching the channel.
 * @property isTyping Whether the user is typing in the channel.
 * @property lastTypingEvent When the last typing event was sent.
 * @property lastReadAt When the user last read the channel.
 * @property lastReadMessageId The ID of the last message the user read.
 * @property unreadCount The number of unread messages.
 * @property watcherCount The number of users watching the channel.
 * @property typingUsers The list of users currently typing in the channel.
 * @property read The map of user IDs to their last read timestamps.
 * @property messages The list of messages in the channel.
 * @property pinnedMessages The list of pinned messages in the channel.
 * @property threadParticipants The map of thread message IDs to their participants.
 */
@Serializable
data class ChannelState(
    val isUpToDate: Boolean = false,
    val isInitialized: Boolean = false,
    val isWatching: Boolean = false,
    val isTyping: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val lastTypingEvent: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val lastReadAt: Instant? = null,
    val lastReadMessageId: String? = null,
    val unreadCount: Int = 0,
    val watcherCount: Int = 0,
    val typingUsers: Map<String, User> = emptyMap(),
    val read: Map<String, Instant> = emptyMap(),
    val messages: List<Message> = emptyList(),
    val pinnedMessages: List<Message> = emptyList(),
    val threadParticipants: Map<String, List<User>> = emptyMap()
)

/**
 * Represents a message in a chat channel.
 *
 * @property id The unique string identifier of the message. This is either created by Stream
 * or set on the client side when the message is added.
 * @property cid Channel unique identifier in <type>:<id> format.
 * @property text The text of this message.
 * @property html The message text formatted as HTML.
 * @property parentId The ID of the parent message, if the message is a thread reply.
 * @property command Contains provided slash command.
 * @property attachments The list of message attachments.
 * @property mentionedUsersIds The list of user IDs mentioned in the message.
 * @property mentionedUsers The list of users mentioned in the message.
 * @property replyCount The number of replies to this message.
 * @property deletedReplyCount The number of deleted replies to this message.
 * @property reactionCounts A mapping between reaction type and the count, ie like:10, heart:4.
 * @property reactionScores A mapping between reaction type and the reaction score.
 * @property reactionGroups A mapping between reaction type and the [ReactionGroup].
 * @property syncStatus If the message has been synced to the servers, default is synced.
 * @property type Contains type of the message. Can be one of: regular, ephemeral, error, reply, system, deleted.
 * @property latestReactions List of the latest reactions to this message.
 * @property ownReactions List of reactions of authenticated user to this message.
 * @property createdAt When the message was created.
 * @property updatedAt When the message was updated.
 * @property deletedAt When the message was deleted.
 * @property updatedLocallyAt When the message was updated locally.
 * @property createdLocallyAt When the message was created locally.
 * @property user The user who sent the message.
 * @property extraData All the custom data provided for this message.
 * @property silent Whether message is silent or not.
 * @property shadowed If the message was sent by shadow banned user.
 * @property i18n Mapping with translations. Key `language` contains the original language key.
 * @property showInChannel Whether thread reply should be shown in the channel as well.
 * @property channelInfo Contains information about the channel where the message was sent.
 * @property replyTo Contains quoted message.
 * @property replyMessageId The ID of the quoted message, if the message is a quoted reply.
 * @property pinned Whether message is pinned or not.
 * @property pinnedAt Date when the message got pinned.
 * @property pinExpires Date when pinned message expires.
 * @property pinnedBy Contains user who pinned the message.
 * @property threadParticipants The list of users who participate in thread.
 * @property skipPushNotification If the message should skip triggering a push notification when sent.
 * @property skipEnrichUrl If the message should skip enriching the URL.
 * @property moderationDetails Contains moderation details of the message (used by moderation v1).
 * @property moderation Contains moderation details of the message (used by moderation v2).
 * @property messageTextUpdatedAt Date when the message text was updated.
 * @property poll Contains poll configuration.
 * @property restrictedVisibility List of user ids that are allowed to see the message.
 */
@Serializable
data class Message(
    val id: String = "",
    val cid: String = "",
    val text: String = "",
    val html: String = "",
    val parentId: String? = null,
    val command: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val mentionedUsersIds: List<String> = emptyList(),
    val mentionedUsers: List<User> = emptyList(),
    val replyCount: Int = 0,
    val deletedReplyCount: Int = 0,
    val reactionCounts: Map<String, Int> = emptyMap(),
    val reactionScores: Map<String, Int> = emptyMap(),
    val reactionGroups: Map<String, ReactionGroup> = emptyMap(),
    val syncStatus: SyncStatus = SyncStatus.COMPLETED,
    val type: String = "",
    val latestReactions: List<Reaction> = emptyList(),
    val ownReactions: List<Reaction> = emptyList(),
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val deletedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedLocallyAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val createdLocallyAt: Instant? = null,
    val user: User = User(),
    @Serializable(with = MapSerializer::class)
    val extraData: Map<String, JsonElement> = emptyMap(),
    val silent: Boolean = false,
    val shadowed: Boolean = false,
    val i18n: Map<String, String> = emptyMap(),
    val showInChannel: Boolean = false,
    val channelInfo: ChannelInfo? = null,
    val replyTo: Message? = null,
    val replyMessageId: String? = null,
    val pinned: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val pinnedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val pinExpires: Instant? = null,
    val pinnedBy: User? = null,
    val threadParticipants: List<User> = emptyList(),
    val skipPushNotification: Boolean = false,
    val skipEnrichUrl: Boolean = false,
    val moderationDetails: MessageModerationDetails? = null,
    val moderation: Moderation? = null,
    @Serializable(with = InstantSerializer::class)
    val messageTextUpdatedAt: Instant? = null,
    val poll: Poll? = null,
    val restrictedVisibility: List<String> = emptyList()
) {
    /**
     * Identifier of message. The message can't be considered the same if the id of the message AND the id of a
     * quoted message are not the same.
     */
    fun identifierHash(): Long {
        var result = id.hashCode()
        replyTo?.id.hashCode().takeIf { it != 0 }?.let { replyHash ->
            result = 31 * result + replyHash
        }
        return result.toLong()
    }
}

/**
 * Represents an attachment in a message. Most commonly these are files, images,
 * videos and audio recordings, but the class is flexible enough that it can represent
 * other things as well such as a date, a given location or other things.
 *
 * @property authorName The name of the site the URL leads to.
 * @property authorLink The link to the website.
 * @property titleLink The link to the URL or the resource linked.
 * @property thumbUrl The URL for the thumbnail version of the attachment.
 * @property imageUrl The URL for the raw version of the attachment.
 * @property assetUrl The URL for the asset.
 * @property ogUrl The original link that was enriched.
 * @property mimeType The mime type of the given attachment. e.g. "image/jpeg"
 * @property fileSize The size of the given attachment.
 * @property title The title of the attachment.
 * @property text The page description.
 * @property type The type of the attachment. e.g "file", "image, "audio".
 * @property image The image attachment.
 * @property name The attachment name.
 * @property fallback Alternative description in the case of an image attachment.
 * @property originalHeight The original height of the attachment.
 * @property originalWidth The original width of the attachment.
 * @property uploadState The state of the upload, i.e. the current progress of uploading the file.
 * @property extraData Stores various extra information that can be sent when uploading the attachment
 * or read when downloading it.
 */
@Serializable
data class Attachment(
    val authorName: String? = null,
    val authorLink: String? = null,
    val titleLink: String? = null,
    val thumbUrl: String? = null,
    val imageUrl: String? = null,
    val assetUrl: String? = null,
    val ogUrl: String? = null,
    val mimeType: String? = null,
    val fileSize: Int = 0,
    val title: String? = null,
    val text: String? = null,
    val type: String? = null,
    val image: String? = null,
    val name: String? = null,
    val fallback: String? = null,
    val originalHeight: Int? = null,
    val originalWidth: Int? = null,
    val uploadState: UploadState? = null,
    @Serializable(with = MapSerializer::class)
    val extraData: Map<String, JsonElement> = emptyMap()
)

/**
 * Represents the state of an attachment upload.
 */
@Serializable
sealed class UploadState {
    @Serializable
    object Idle : UploadState()
    
    @Serializable
    data class InProgress(
        val bytesUploaded: Long,
        val totalBytes: Long
    ) : UploadState()
    
    @Serializable
    object Success : UploadState()
    
    @Serializable
    data class Failed(
        val error: String
    ) : UploadState()
}

/**
 * Represents a reaction to a message.
 *
 * @property type The type of reaction (e.g. "like", "heart").
 * @property score The score of the reaction.
 * @property messageId The ID of the message this reaction belongs to.
 * @property user The user who added the reaction.
 * @property createdAt When the reaction was created.
 * @property updatedAt When the reaction was updated.
 * @property extraData Additional custom data for the reaction.
 */
@Serializable
data class Reaction(
    val type: String,
    val score: Int = 1,
    val messageId: String,
    val user: User,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
    @Serializable(with = MapSerializer::class)
    val extraData: Map<String, JsonElement> = emptyMap()
)

/**
 * Represents a group of reactions of the same type.
 *
 * @property count The number of reactions in this group.
 * @property score The total score of reactions in this group.
 * @property users The list of users who added these reactions.
 */
@Serializable
data class ReactionGroup(
    val count: Int,
    val score: Int,
    val users: List<User>
)

/**
 * Represents the sync status of a message.
 */
@Serializable
enum class SyncStatus {
    @SerialName("completed")
    COMPLETED,
    
    @SerialName("pending")
    PENDING,
    
    @SerialName("failed")
    FAILED
}

/**
 * Represents moderation details for a message (v1).
 *
 * @property action The moderation action taken.
 * @property reason The reason for the moderation action.
 * @property details Additional details about the moderation.
 */
@Serializable
data class MessageModerationDetails(
    val action: String,
    val reason: String,
    val details: Map<String, String> = emptyMap()
)

/**
 * Represents moderation details for a message (v2).
 *
 * @property status The moderation status.
 * @property action The moderation action taken.
 * @property reason The reason for the moderation action.
 * @property details Additional details about the moderation.
 */
@Serializable
data class Moderation(
    val status: String,
    val action: String,
    val reason: String,
    val details: Map<String, String> = emptyMap()
)

/**
 * Represents a poll in a message.
 *
 * @property id The unique identifier of the poll.
 * @property question The poll question.
 * @property options The list of poll options.
 * @property settings The poll settings.
 * @property status The current status of the poll.
 * @property createdAt When the poll was created.
 * @property updatedAt When the poll was updated.
 * @property closedAt When the poll was closed.
 * @property createdBy The user who created the poll.
 * @property extraData Additional custom data for the poll.
 */
@Serializable
data class Poll(
    val id: String,
    val question: String,
    val options: List<PollOption>,
    val settings: PollSettings,
    val status: PollStatus = PollStatus.ACTIVE,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val closedAt: Instant? = null,
    val createdBy: User,
    @Serializable(with = MapSerializer::class)
    val extraData: Map<String, JsonElement> = emptyMap()
)

/**
 * Represents a poll option.
 *
 * @property id The unique identifier of the option.
 * @property text The option text.
 * @property votes The number of votes for this option.
 * @property voters The list of users who voted for this option.
 */
@Serializable
data class PollOption(
    val id: String,
    val text: String,
    val votes: Int = 0,
    val voters: List<User> = emptyList()
)

/**
 * Represents settings for a poll.
 *
 * @property allowMultipleVotes Whether users can vote for multiple options.
 * @property allowAddOptions Whether users can add new options.
 * @property allowComments Whether users can comment on the poll.
 * @property endDate When the poll should end.
 * @property showResults Whether to show results before the poll ends.
 */
@Serializable
data class PollSettings(
    val allowMultipleVotes: Boolean = false,
    val allowAddOptions: Boolean = false,
    val allowComments: Boolean = true,
    @Serializable(with = InstantSerializer::class)
    val endDate: Instant? = null,
    val showResults: Boolean = true
)

/**
 * Represents the status of a poll.
 */
@Serializable
enum class PollStatus {
    @SerialName("active")
    ACTIVE,
    
    @SerialName("closed")
    CLOSED,
    
    @SerialName("expired")
    EXPIRED
}

/**
 * Represents information about a channel.
 *
 * @property cid The channel ID in the format "type:id".
 * @property type The channel type.
 * @property id The channel ID.
 * @property name The channel name.
 * @property image The channel image URL.
 * @property extraData Additional custom data for the channel.
 */
@Serializable
data class ChannelInfo(
    val cid: String,
    val type: String,
    val id: String,
    val name: String? = null,
    val image: String? = null,
    @Serializable(with = MapSerializer::class)
    val extraData: Map<String, JsonElement> = emptyMap()
)

/**
 * Represents a ban applied to a user in a channel.
 *
 * @property user The user who is banned.
 * @property bannedBy The user who applied the ban.
 * @property channel The channel where the ban is applied.
 * @property reason The reason for the ban.
 * @property createdAt When the ban was created.
 * @property expiresAt When the ban expires, if applicable.
 * @property shadow Whether this is a shadow ban.
 */
@Serializable
data class Ban(
    val user: User,
    val bannedBy: User? = null,
    val channel: Channel? = null,
    val reason: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val expiresAt: Instant? = null,
    val shadow: Boolean = false
) 