package io.getstream.chat.client

import kotlinx.browser.window
import kotlinx.serialization.Serializable
import org.w3c.dom.navigator

/**
 * JavaScript implementation of [ChatClientConfig.PlatformConfig].
 */
@Serializable
actual class ChatClientConfig.PlatformConfig actual constructor(
    actual val isDebug: Boolean,
    actual val userAgent: String,
    actual val sslConfig: ChatClientConfig.SslConfig
) {
    /**
     * JavaScript implementation of [ChatClientConfig.PlatformConfig.Builder].
     */
    actual class Builder {
        private var isDebug: Boolean = false
        private var userAgent: String = "Stream Chat JS SDK"
        private var sslConfig: ChatClientConfig.SslConfig = ChatClientConfig.SslConfig()
        private var version: String = "unknown"
        private var isNodeJs: Boolean = js("typeof process !== 'undefined' && process.versions != null && process.versions.node != null") as Boolean

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
            if (userAgent == "Stream Chat JS SDK") {
                userAgent = generateUserAgent()
            }

            return ChatClientConfig.PlatformConfig(
                isDebug = isDebug,
                userAgent = userAgent,
                sslConfig = sslConfig
            )
        }

        private fun generateUserAgent(): String {
            return if (isNodeJs) {
                generateNodeJsUserAgent()
            } else {
                generateBrowserUserAgent()
            }
        }

        private fun generateNodeJsUserAgent(): String {
            val process = js("process")
            val nodeVersion = process.versions.node as String
            val platform = process.platform as String
            val arch = process.arch as String

            return "Stream Chat JS SDK/$version " +
                "Node.js/$nodeVersion " +
                "($platform; $arch)"
        }

        private fun generateBrowserUserAgent(): String {
            val browserInfo = detectBrowser()
            val osInfo = detectOperatingSystem()

            return "Stream Chat JS SDK/$version " +
                "${browserInfo.name}/${browserInfo.version} " +
                "(${osInfo.name} ${osInfo.version})"
        }

        private fun detectBrowser(): BrowserInfo {
            val userAgent = window.navigator.userAgent.lowercase()
            
            return when {
                userAgent.contains("chrome") && !userAgent.contains("edg") -> {
                    val version = extractVersion(userAgent, "chrome/")
                    BrowserInfo("Chrome", version)
                }
                userAgent.contains("firefox") -> {
                    val version = extractVersion(userAgent, "firefox/")
                    BrowserInfo("Firefox", version)
                }
                userAgent.contains("safari") && !userAgent.contains("chrome") -> {
                    val version = extractVersion(userAgent, "version/")
                    BrowserInfo("Safari", version)
                }
                userAgent.contains("edg") -> {
                    val version = extractVersion(userAgent, "edg/")
                    BrowserInfo("Edge", version)
                }
                else -> BrowserInfo("Unknown", "unknown")
            }
        }

        private fun detectOperatingSystem(): OsInfo {
            val userAgent = window.navigator.userAgent.lowercase()
            val platform = window.navigator.platform.lowercase()
            
            return when {
                userAgent.contains("windows") -> OsInfo("Windows", extractVersion(userAgent, "windows nt "))
                userAgent.contains("mac") -> OsInfo("macOS", extractVersion(userAgent, "mac os x "))
                userAgent.contains("linux") -> OsInfo("Linux", "unknown")
                platform.contains("iphone") || platform.contains("ipad") -> {
                    val version = extractVersion(userAgent, "os ")
                    OsInfo("iOS", version)
                }
                platform.contains("android") -> {
                    val version = extractVersion(userAgent, "android ")
                    OsInfo("Android", version)
                }
                else -> OsInfo("Unknown", "unknown")
            }
        }

        private fun extractVersion(userAgent: String, prefix: String): String {
            val startIndex = userAgent.indexOf(prefix) + prefix.length
            val endIndex = userAgent.indexOf(" ", startIndex)
            return if (startIndex > prefix.length && endIndex > startIndex) {
                userAgent.substring(startIndex, endIndex)
            } else {
                "unknown"
            }
        }

        private data class BrowserInfo(val name: String, val version: String)
        private data class OsInfo(val name: String, val version: String)
    }
} 