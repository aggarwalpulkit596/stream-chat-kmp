package io.getstream.chat.auth

import platform.Foundation.*
import platform.Security.*
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS implementation of [TokenStorage] using Keychain.
 * Provides secure storage for sensitive data like authentication tokens.
 */
actual class PlatformTokenStorage : TokenStorage {
    private val service = "io.getstream.chat"
    private val accessGroup: String? = null // Set this if you need shared keychain access

    actual override suspend fun initialize() {
        // No initialization needed for Keychain
    }

    actual override suspend fun store(key: String, value: String) {
        withContext(Dispatchers.Default) {
            val query = mutableMapOf<String, Any>().apply {
                put(kSecClass as String, kSecClassGenericPassword)
                put(kSecAttrService as String, service)
                put(kSecAttrAccount as String, key)
                put(kSecValueData as String, value.encodeToByteArray().toNSData())
                put(kSecAttrAccessible as String, kSecAttrAccessibleAfterFirstUnlock)
                accessGroup?.let { put(kSecAttrAccessGroup as String, it) }
            }

            // First try to delete any existing item
            SecItemDelete(query.toCFDictionary())

            // Then add the new item
            val status = SecItemAdd(query.toCFDictionary(), null)
            if (status != errSecSuccess.toInt()) {
                throw SecurityException("Failed to store item in Keychain: $status")
            }
        }
    }

    actual override suspend fun retrieve(key: String): String? {
        return withContext(Dispatchers.Default) {
            val query = mutableMapOf<String, Any>().apply {
                put(kSecClass as String, kSecClassGenericPassword)
                put(kSecAttrService as String, service)
                put(kSecAttrAccount as String, key)
                put(kSecReturnData as String, true)
                put(kSecMatchLimit as String, kSecMatchLimitOne)
                accessGroup?.let { put(kSecAttrAccessGroup as String, it) }
            }

            val result = memScoped {
                val dataPtr = alloc<COpaquePointerVar>()
                val status = SecItemCopyMatching(query.toCFDictionary(), dataPtr.ptr)
                
                if (status == errSecSuccess.toInt()) {
                    val data = dataPtr.value?.asStableRef<NSData>()?.get()
                    data?.toByteArray()?.toString(Charsets.UTF_8)
                } else if (status == errSecItemNotFound.toInt()) {
                    null
                } else {
                    throw SecurityException("Failed to retrieve item from Keychain: $status")
                }
            }

            result
        }
    }

    actual override suspend fun clear(key: String) {
        withContext(Dispatchers.Default) {
            val query = mutableMapOf<String, Any>().apply {
                put(kSecClass as String, kSecClassGenericPassword)
                put(kSecAttrService as String, service)
                put(kSecAttrAccount as String, key)
                accessGroup?.let { put(kSecAttrAccessGroup as String, it) }
            }

            val status = SecItemDelete(query.toCFDictionary())
            if (status != errSecSuccess.toInt() && status != errSecItemNotFound.toInt()) {
                throw SecurityException("Failed to delete item from Keychain: $status")
            }
        }
    }

    private fun Map<String, Any>.toCFDictionary(): CFDictionaryRef {
        val keys = keys.map { it as CFStringRef }.toTypedArray()
        val values = values.map { it as CFTypeRef }.toTypedArray()
        return CFDictionaryCreate(
            kCFAllocatorDefault,
            keys.refTo(0),
            values.refTo(0),
            size.toLong(),
            null,
            null
        )
    }

    private fun ByteArray.toNSData(): NSData {
        return NSData.dataWithBytes(this, this.size.toULong())
    }

    private fun NSData.toByteArray(): ByteArray {
        return ByteArray(length.toInt()) { index ->
            bytes?.get(index)?.toByte() ?: 0
        }
    }
}

/**
 * Exception thrown when Keychain operations fail.
 */
class SecurityException(message: String) : Exception(message) 