package io.getstream.chat.client

import kotlinx.serialization.Serializable
import java.util.Properties

/**
 * Desktop implementation of [ChatClientConfig.PlatformConfig].
 */
@Serializable
actual class ChatClientConfig.PlatformConfig actual constructor(
    actual val isDebug: Boolean,
    actual val userAgent: String,
    actual val sslConfig: ChatClientConfig.SslConfig
) {
    /**
     * Desktop implementation of [ChatClientConfig.PlatformConfig.Builder].
     */
    actual class Builder {
        private var isDebug: Boolean = false
        private var userAgent: String = "Stream Chat Desktop SDK"
        private var sslConfig: ChatClientConfig.SslConfig = ChatClientConfig.SslConfig()
        private var version: String = "unknown"

        /**
         * Sets the SDK version.
         * This is required for proper user agent generation.
         */
        fun version(version: String) = apply { this.version = version }

        actual fun isDebug(debug: Boolean) = apply { this.isDebug = debug }

        actual fun userAgent(userAgent: String) = apply { this.userAgent = userAgent }

        actual fun sslConfig(block: ChatClientConfig.SslConfig.Builder.() -> Unit) = apply {
            this.sslConfig = ChatClientConfig.SslConfig.Builder().apply(block).build()
        }

        actual fun build(): ChatClientConfig.PlatformConfig {
            // Generate user agent if not explicitly set
            if (userAgent == "Stream Chat Desktop SDK") {
                userAgent = generateUserAgent()
            }

            return ChatClientConfig.PlatformConfig(
                isDebug = isDebug,
                userAgent = userAgent,
                sslConfig = sslConfig
            )
        }

        private fun generateUserAgent(): String {
            val osInfo = detectOperatingSystem()
            val javaVersion = System.getProperty("java.version")
            val javaVendor = System.getProperty("java.vendor")
            val javaRuntime = System.getProperty("java.runtime.name")

            return "Stream Chat Desktop SDK/$version " +
                "Java/$javaVersion ($javaVendor; $javaRuntime) " +
                "(${osInfo.name} ${osInfo.version}; ${osInfo.arch})"
        }

        private fun detectOperatingSystem(): OsInfo {
            val osName = System.getProperty("os.name")
            val osVersion = System.getProperty("os.version")
            val osArch = System.getProperty("os.arch")

            return OsInfo(
                name = osName,
                version = osVersion,
                arch = osArch
            )
        }

        private data class OsInfo(
            val name: String,
            val version: String,
            val arch: String
        )
    }
} 