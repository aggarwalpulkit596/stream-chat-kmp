package io.getstream.chat.client

import platform.Foundation.*
import platform.UIKit.*
import kotlinx.serialization.Serializable

/**
 * iOS implementation of [ChatClientConfig.PlatformConfig].
 */
@Serializable
actual class ChatClientConfig.PlatformConfig actual constructor(
    actual val isDebug: Boolean,
    actual val userAgent: String,
    actual val sslConfig: ChatClientConfig.SslConfig
) {
    /**
     * iOS implementation of [ChatClientConfig.PlatformConfig.Builder].
     */
    actual class Builder {
        private var isDebug: Boolean = false
        private var userAgent: String = "Stream Chat iOS SDK"
        private var sslConfig: ChatClientConfig.SslConfig = ChatClientConfig.SslConfig()

        actual fun isDebug(debug: Boolean) = apply { this.isDebug = debug }

        actual fun userAgent(userAgent: String) = apply { this.userAgent = userAgent }

        actual fun sslConfig(block: ChatClientConfig.SslConfig.Builder.() -> Unit) = apply {
            this.sslConfig = ChatClientConfig.SslConfig.Builder().apply(block).build()
        }

        actual fun build(): ChatClientConfig.PlatformConfig {
            // Generate user agent if not explicitly set
            if (userAgent == "Stream Chat iOS SDK") {
                userAgent = generateUserAgent()
            }

            return ChatClientConfig.PlatformConfig(
                isDebug = isDebug,
                userAgent = userAgent,
                sslConfig = sslConfig
            )
        }

        private fun generateUserAgent(): String {
            val bundle = NSBundle.mainBundle
            val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown"
            val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "unknown"
            
            val device = UIDevice.currentDevice
            val systemVersion = device.systemVersion
            val model = device.model
            val name = device.name
            
            return "Stream Chat iOS SDK/$version ($build) " +
                "iOS/$systemVersion " +
                "($model; $name)"
        }
    }
} 