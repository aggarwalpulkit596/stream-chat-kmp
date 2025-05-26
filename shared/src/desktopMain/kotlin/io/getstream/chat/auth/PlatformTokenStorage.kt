package io.getstream.chat.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * Desktop implementation of [TokenStorage] using encrypted file storage.
 * Provides secure storage for sensitive data like authentication tokens.
 */
actual class PlatformTokenStorage : TokenStorage {
    private val storageDir = Paths.get(System.getProperty("user.home"), ".stream-chat", "storage")
    private val keyFile = storageDir.resolve("key")
    private var secretKey: SecretKey? = null

    actual override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            // Create storage directory if it doesn't exist
            Files.createDirectories(storageDir)
            
            // Load or generate encryption key
            secretKey = loadOrGenerateKey()
        }
    }

    actual override suspend fun store(key: String, value: String) {
        withContext(Dispatchers.IO) {
            checkInitialized()
            
            val encryptedValue = encrypt(value)
            val file = storageDir.resolve(key).toFile()
            
            file.writeText(encryptedValue)
            file.setReadable(true, true)
            file.setWritable(true, true)
        }
    }

    actual override suspend fun retrieve(key: String): String? {
        return withContext(Dispatchers.IO) {
            checkInitialized()
            
            val file = storageDir.resolve(key).toFile()
            if (!file.exists()) return@withContext null
            
            try {
                val encryptedValue = file.readText()
                decrypt(encryptedValue)
            } catch (e: Exception) {
                null
            }
        }
    }

    actual override suspend fun clear(key: String) {
        withContext(Dispatchers.IO) {
            checkInitialized()
            
            val file = storageDir.resolve(key).toFile()
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun loadOrGenerateKey(): SecretKey {
        return if (keyFile.toFile().exists()) {
            // Load existing key
            val keyBytes = Files.readAllBytes(keyFile)
            SecretKeySpec(keyBytes, "AES")
        } else {
            // Generate new key
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256, SecureRandom())
            val key = keyGenerator.generateKey()
            
            // Save key
            Files.write(keyFile, key.encoded)
            keyFile.toFile().setReadable(true, true)
            keyFile.toFile().setWritable(true, true)
            
            key
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val parameterSpec = GCMParameterSpec(128, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        
        // Combine IV and encrypted data
        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)
        
        return Base64.getEncoder().encodeToString(result)
    }

    private fun decrypt(value: String): String {
        val data = Base64.getDecoder().decode(value)
        
        // Extract IV and encrypted data
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(128, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        val decrypted = cipher.doFinal(encrypted)
        
        return String(decrypted, Charsets.UTF_8)
    }

    private fun checkInitialized() {
        check(secretKey != null) { "PlatformTokenStorage not initialized. Call initialize() first." }
    }
}

/**
 * Exception thrown when security operations fail.
 */
class SecurityException(message: String) : Exception(message) 