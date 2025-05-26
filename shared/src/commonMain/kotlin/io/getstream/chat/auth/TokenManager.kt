package io.getstream.chat.auth

import io.getstream.chat.client.ChatApiClient
import io.getstream.chat.models.ApiError
import io.getstream.chat.models.ApiException
import io.getstream.chat.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Manages authentication tokens and user sessions for Stream Chat.
 * Handles token storage, validation, refresh, and user session state.
 *
 * @property tokenStorage Platform-specific token storage implementation
 * @property apiClient The API client for token operations
 * @property tokenRefreshThreshold Duration before token expiration to trigger refresh
 */
class TokenManager(
    private val tokenStorage: TokenStorage,
    private val apiClient: ChatApiClient,
    private val tokenRefreshThreshold: Duration = 5.minutes
) {
    companion object {
        private const val TOKEN_KEY = "stream_chat_token"
        private const val USER_KEY = "stream_chat_user"
        private const val TOKEN_EXPIRY_KEY = "stream_chat_token_expiry"
    }

    /**
     * Current authentication state.
     */
    sealed class AuthState {
        /**
         * No user is authenticated.
         */
        object NotAuthenticated : AuthState()

        /**
         * User is authenticated anonymously.
         */
        data class Anonymous(val user: User) : AuthState()

        /**
         * User is fully authenticated.
         */
        data class Authenticated(val user: User) : AuthState()

        /**
         * Authentication is in progress.
         */
        object Authenticating : AuthState()

        /**
         * Authentication failed.
         */
        data class Error(val error: ApiError) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Current user if authenticated, null otherwise.
     */
    val currentUser: User?
        get() = when (val state = _authState.value) {
            is AuthState.Anonymous -> state.user
            is AuthState.Authenticated -> state.user
            else -> null
        }

    /**
     * Whether a user is currently authenticated.
     */
    val isAuthenticated: Boolean
        get() = _authState.value is AuthState.Authenticated

    /**
     * Whether the current user is anonymous.
     */
    val isAnonymous: Boolean
        get() = _authState.value is AuthState.Anonymous

    /**
     * Authenticates a user with the provided token.
     *
     * @param userId The ID of the user to authenticate
     * @param token The JWT token for the user
     * @param isAnonymous Whether this is an anonymous user
     * @return The authenticated user
     * @throws ApiException if authentication fails
     */
    suspend fun authenticate(
        userId: String,
        token: String,
        isAnonymous: Boolean = false
    ): User {
        _authState.value = AuthState.Authenticating

        try {
            // Validate token format
            validateToken(token)

            // Connect user through API
            val user = apiClient.connectUser(userId, token)

            // Store token and user info
            tokenStorage.store(TOKEN_KEY, token)
            tokenStorage.store(USER_KEY, Json.encodeToString(User.serializer(), user))
            tokenStorage.store(TOKEN_EXPIRY_KEY, getTokenExpiry(token).toString())

            // Update auth state
            _authState.value = if (isAnonymous) {
                AuthState.Anonymous(user)
            } else {
                AuthState.Authenticated(user)
            }

            return user
        } catch (e: Exception) {
            val error = when (e) {
                is ApiException -> e.error
                else -> ApiError(
                    code = 0,
                    message = "Authentication failed: ${e.message}",
                    statusCode = 401
                )
            }
            _authState.value = AuthState.Error(error)
            throw ApiException(error)
        }
    }

    /**
     * Authenticates a user anonymously.
     *
     * @param userId The ID for the anonymous user
     * @return The anonymous user
     * @throws ApiException if authentication fails
     */
    suspend fun authenticateAnonymously(userId: String): User {
        // Generate anonymous token (implementation depends on your backend)
        val token = generateAnonymousToken(userId)
        return authenticate(userId, token, isAnonymous = true)
    }

    /**
     * Refreshes the current user's token.
     *
     * @return The new token
     * @throws ApiException if refresh fails
     */
    suspend fun refreshToken(): String {
        val currentToken = tokenStorage.retrieve(TOKEN_KEY)
            ?: throw ApiException(ApiError(code = 0, message = "No token to refresh", statusCode = 401))

        try {
            // Call your backend to refresh the token
            val newToken = refreshTokenOnBackend(currentToken)
            
            // Validate new token
            validateToken(newToken)
            
            // Store new token
            tokenStorage.store(TOKEN_KEY, newToken)
            tokenStorage.store(TOKEN_EXPIRY_KEY, getTokenExpiry(newToken).toString())
            
            return newToken
        } catch (e: Exception) {
            val error = when (e) {
                is ApiException -> e.error
                else -> ApiError(
                    code = 0,
                    message = "Token refresh failed: ${e.message}",
                    statusCode = 401
                )
            }
            _authState.value = AuthState.Error(error)
            throw ApiException(error)
        }
    }

    /**
     * Logs out the current user.
     */
    suspend fun logout() {
        try {
            currentUser?.let { user ->
                apiClient.disconnectUser()
            }
        } finally {
            // Clear stored data
            tokenStorage.clear(TOKEN_KEY)
            tokenStorage.clear(USER_KEY)
            tokenStorage.clear(TOKEN_EXPIRY_KEY)
            _authState.value = AuthState.NotAuthenticated
        }
    }

    /**
     * Checks if the current token needs refresh.
     */
    suspend fun checkTokenRefresh(): Boolean {
        val token = tokenStorage.retrieve(TOKEN_KEY) ?: return false
        val expiryStr = tokenStorage.retrieve(TOKEN_EXPIRY_KEY) ?: return false
        
        val expiry = Instant.parse(expiryStr)
        val now = Clock.System.now()
        
        return (expiry - now) <= tokenRefreshThreshold
    }

    /**
     * Gets the current token, refreshing if necessary.
     *
     * @return The current valid token
     * @throws ApiException if token is invalid or refresh fails
     */
    suspend fun getValidToken(): String {
        val token = tokenStorage.retrieve(TOKEN_KEY)
            ?: throw ApiException(ApiError(code = 0, message = "No token available", statusCode = 401))

        if (checkTokenRefresh()) {
            return refreshToken()
        }

        return token
    }

    /**
     * Restores the previous session if available.
     *
     * @return The restored user if successful, null otherwise
     */
    suspend fun restoreSession(): User? {
        val token = tokenStorage.retrieve(TOKEN_KEY) ?: return null
        val userJson = tokenStorage.retrieve(USER_KEY) ?: return null
        
        try {
            val user = Json.decodeFromString(User.serializer(), userJson)
            
            // Validate token
            validateToken(token)
            
            // Check if token needs refresh
            if (checkTokenRefresh()) {
                refreshToken()
            }
            
            // Update auth state
            _authState.value = if (user.role == "anonymous") {
                AuthState.Anonymous(user)
            } else {
                AuthState.Authenticated(user)
            }
            
            return user
        } catch (e: Exception) {
            // Clear invalid session
            logout()
            return null
        }
    }

    private fun validateToken(token: String) {
        try {
            // Basic JWT format validation
            require(token.matches(Regex("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$"))) {
                "Invalid token format"
            }

            // Parse and validate token claims
            val claims = parseTokenClaims(token)
            require(claims["exp"] != null) { "Token missing expiration" }
            require(claims["user_id"] != null) { "Token missing user ID" }
        } catch (e: Exception) {
            throw ApiException(
                ApiError(
                    code = 0,
                    message = "Invalid token: ${e.message}",
                    statusCode = 401
                )
            )
        }
    }

    private fun getTokenExpiry(token: String): Instant {
        val claims = parseTokenClaims(token)
        val exp = claims["exp"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: throw ApiException(ApiError(code = 0, message = "Invalid token expiration", statusCode = 401))
        return Instant.fromEpochSeconds(exp)
    }

    private fun parseTokenClaims(token: String): JsonObject {
        val parts = token.split(".")
        require(parts.size >= 2) { "Invalid token format" }
        
        val claimsBase64 = parts[1]
        val claimsJson = base64UrlDecode(claimsBase64)
        return Json.parseToJsonElement(claimsJson).jsonObject
    }

    private fun base64UrlDecode(str: String): String {
        val base64 = str.replace('-', '+').replace('_', '/')
        val padding = "=".repeat((4 - base64.length % 4) % 4)
        return base64 + padding
    }

    private suspend fun generateAnonymousToken(userId: String): String {
        // Implement based on your backend's anonymous token generation
        // This is just a placeholder
        throw NotImplementedError("Anonymous token generation must be implemented")
    }

    private suspend fun refreshTokenOnBackend(currentToken: String): String {
        // Implement based on your backend's token refresh endpoint
        // This is just a placeholder
        throw NotImplementedError("Token refresh must be implemented")
    }
}

/**
 * Platform-specific token storage interface.
 * Implementations should handle secure storage of sensitive data.
 */
interface TokenStorage {
    /**
     * Stores a value securely.
     *
     * @param key The key to store the value under
     * @param value The value to store
     */
    suspend fun store(key: String, value: String)

    /**
     * Retrieves a stored value.
     *
     * @param key The key to retrieve
     * @return The stored value, or null if not found
     */
    suspend fun retrieve(key: String): String?

    /**
     * Clears a stored value.
     *
     * @param key The key to clear
     */
    suspend fun clear(key: String)
}

/**
 * Authentication-related exceptions.
 */
sealed class AuthException : Exception() {
    /**
     * Token is invalid or expired.
     */
    data class InvalidToken(override val message: String) : AuthException()

    /**
     * Token refresh failed.
     */
    data class RefreshFailed(override val message: String) : AuthException()

    /**
     * User is not authenticated.
     */
    object NotAuthenticated : AuthException() {
        override val message: String = "User is not authenticated"
    }

    /**
     * Authentication failed.
     */
    data class AuthenticationFailed(override val message: String) : AuthException()
}

/**
 * Platform-specific implementation of [TokenStorage].
 * This is an expect class that must be implemented for each platform.
 */
expect class PlatformTokenStorage() : TokenStorage {
    /**
     * Initializes the token storage.
     * This should be called before using the storage.
     */
    suspend fun initialize()
} 