package io.getstream.chat.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [TokenStorage] using [EncryptedSharedPreferences].
 * Provides secure storage for sensitive data like authentication tokens.
 *
 * @property context The application context
 */
actual class PlatformTokenStorage(
    private val context: Context
) : TokenStorage {
    private var encryptedPrefs: EncryptedSharedPreferences? = null

    actual override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "stream_chat_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        }
    }

    actual override suspend fun store(key: String, value: String) {
        withContext(Dispatchers.IO) {
            checkInitialized()
            encryptedPrefs?.edit()?.putString(key, value)?.apply()
        }
    }

    actual override suspend fun retrieve(key: String): String? {
        return withContext(Dispatchers.IO) {
            checkInitialized()
            encryptedPrefs?.getString(key, null)
        }
    }

    actual override suspend fun clear(key: String) {
        withContext(Dispatchers.IO) {
            checkInitialized()
            encryptedPrefs?.edit()?.remove(key)?.apply()
        }
    }

    private fun checkInitialized() {
        check(encryptedPrefs != null) { "PlatformTokenStorage not initialized. Call initialize() first." }
    }
} 