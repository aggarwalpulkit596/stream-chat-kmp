package io.getstream.chat.auth

import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.Window
import kotlin.js.Promise
import kotlin.js.json

/**
 * JavaScript implementation of [TokenStorage].
 * Uses localStorage with encryption for browser environments and secure file storage for Node.js.
 */
actual class PlatformTokenStorage : TokenStorage {
    private val storageKey = "stream_chat_secure_storage"
    private val isNodeJs = js("typeof process !== 'undefined' && process.versions != null && process.versions.node != null") as Boolean
    private val crypto = if (isNodeJs) {
        js("require('crypto')")
    } else {
        window.crypto
    }

    actual override suspend fun initialize() {
        // No initialization needed
    }

    actual override suspend fun store(key: String, value: String) {
        withContext(Dispatchers.Default) {
            val encryptedValue = encrypt(value)
            if (isNodeJs) {
                storeNodeJs(key, encryptedValue)
            } else {
                localStorage.setItem("$storageKey:$key", encryptedValue)
            }
        }
    }

    actual override suspend fun retrieve(key: String): String? {
        return withContext(Dispatchers.Default) {
            val encryptedValue = if (isNodeJs) {
                retrieveNodeJs(key)
            } else {
                localStorage.getItem("$storageKey:$key")
            } ?: return@withContext null

            try {
                decrypt(encryptedValue)
            } catch (e: Exception) {
                null
            }
        }
    }

    actual override suspend fun clear(key: String) {
        withContext(Dispatchers.Default) {
            if (isNodeJs) {
                clearNodeJs(key)
            } else {
                localStorage.removeItem("$storageKey:$key")
            }
        }
    }

    private fun encrypt(value: String): String {
        return if (isNodeJs) {
            encryptNodeJs(value)
        } else {
            encryptBrowser(value)
        }
    }

    private fun decrypt(value: String): String {
        return if (isNodeJs) {
            decryptNodeJs(value)
        } else {
            decryptBrowser(value)
        }
    }

    private fun encryptBrowser(value: String): String {
        // Generate a random key for this session
        val key = crypto.getRandomValues(Uint8Array(32))
        val iv = crypto.getRandomValues(Uint8Array(12))

        // Encrypt the value
        val encodedValue = value.encodeToByteArray()
        val encryptedData = crypto.subtle.encrypt(
            json(
                "name" to "AES-GCM",
                "iv" to iv
            ),
            key,
            encodedValue.toTypedArray()
        ).unsafeCast<ArrayBuffer>()

        // Combine IV and encrypted data
        val result = Uint8Array(iv.length + encryptedData.byteLength)
        result.set(iv, 0)
        result.set(Uint8Array(encryptedData), iv.length)

        // Store the key in memory (not persistent)
        sessionKeys[key.toString()] = key

        return result.toBase64()
    }

    private fun decryptBrowser(value: String): String {
        val data = value.fromBase64()
        val iv = data.slice(0, 12)
        val encryptedData = data.slice(12)

        // Get the key from memory
        val key = sessionKeys[value] ?: throw SecurityException("Session key not found")

        // Decrypt the value
        val decryptedData = crypto.subtle.decrypt(
            json(
                "name" to "AES-GCM",
                "iv" to iv
            ),
            key,
            encryptedData
        ).unsafeCast<ArrayBuffer>()

        return Uint8Array(decryptedData).toByteArray().toString(Charsets.UTF_8)
    }

    private fun encryptNodeJs(value: String): String {
        val key = getOrCreateNodeJsKey()
        val iv = crypto.randomBytes(12)
        val cipher = crypto.createCipheriv("aes-256-gcm", key, iv)
        
        val encrypted = cipher.update(value, "utf8", "base64") + cipher.final("base64")
        val authTag = cipher.getAuthTag()
        
        // Combine IV, auth tag, and encrypted data
        return "${iv.toString("base64")}:${authTag.toString("base64")}:$encrypted"
    }

    private fun decryptNodeJs(value: String): String {
        val (ivBase64, authTagBase64, encrypted) = value.split(":")
        val key = getOrCreateNodeJsKey()
        
        val iv = crypto.createBuffer(ivBase64, "base64")
        val authTag = crypto.createBuffer(authTagBase64, "base64")
        
        val decipher = crypto.createDecipheriv("aes-256-gcm", key, iv)
        decipher.setAuthTag(authTag)
        
        return decipher.update(encrypted, "base64", "utf8") + decipher.final("utf8")
    }

    private fun getOrCreateNodeJsKey(): dynamic {
        val fs = js("require('fs')")
        val path = js("require('path')")
        val os = js("require('os')")
        
        val keyPath = path.join(os.homedir(), ".stream-chat", "key")
        
        return try {
            val key = fs.readFileSync(keyPath)
            crypto.createBuffer(key, "base64")
        } catch (e: dynamic) {
            val key = crypto.randomBytes(32)
            fs.mkdirSync(path.dirname(keyPath), json("recursive" to true))
            fs.writeFileSync(keyPath, key.toString("base64"))
            key
        }
    }

    private fun storeNodeJs(key: String, value: String) {
        val fs = js("require('fs')")
        val path = js("require('path')")
        val os = js("require('os')")
        
        val storagePath = path.join(os.homedir(), ".stream-chat", "storage")
        val filePath = path.join(storagePath, key)
        
        fs.mkdirSync(storagePath, json("recursive" to true))
        fs.writeFileSync(filePath, value)
    }

    private fun retrieveNodeJs(key: String): String? {
        val fs = js("require('fs')")
        val path = js("require('path')")
        val os = js("require('os')")
        
        val filePath = path.join(os.homedir(), ".stream-chat", "storage", key)
        
        return try {
            fs.readFileSync(filePath, "utf8")
        } catch (e: dynamic) {
            null
        }
    }

    private fun clearNodeJs(key: String) {
        val fs = js("require('fs')")
        val path = js("require('path')")
        val os = js("require('os')")
        
        val filePath = path.join(os.homedir(), ".stream-chat", "storage", key)
        
        try {
            fs.unlinkSync(filePath)
        } catch (e: dynamic) {
            // Ignore if file doesn't exist
        }
    }

    private fun Uint8Array.toBase64(): String {
        val binary = this
        val bytes = ByteArray(binary.length)
        for (i in 0 until binary.length) {
            bytes[i] = binary[i].toByte()
        }
        return js("btoa(String.fromCharCode.apply(null, bytes))") as String
    }

    private fun String.fromBase64(): Uint8Array {
        val binary = js("atob(this)") as String
        val bytes = Uint8Array(binary.length)
        for (i in 0 until binary.length) {
            bytes[i] = binary[i].code.toByte()
        }
        return bytes
    }

    private fun Uint8Array.toByteArray(): ByteArray {
        return ByteArray(length) { index -> get(index).toByte() }
    }

    private fun ByteArray.toTypedArray(): Uint8Array {
        return Uint8Array(this)
    }

    companion object {
        private val sessionKeys = mutableMapOf<String, dynamic>()
    }
}

/**
 * Exception thrown when security operations fail.
 */
class SecurityException(message: String) : Exception(message) 