package io.getstream.chat.client

import android.content.Context
import android.os.Build
import kotlinx.serialization.Serializable

/**
 * Android implementation of [ChatClientConfig.PlatformConfig].
 */
@Serializable
actual class ChatClientConfig.PlatformConfig actual constructor(
    actual val isDebug: Boolean,
    actual val userAgent: String,
    actual val sslConfig: ChatClientConfig.SslConfig
) {
    /**
     * Android implementation of [ChatClientConfig.PlatformConfig.Builder].
     */
    actual class Builder {
        private var isDebug: Boolean = false
        private var userAgent: String = "Stream Chat Android SDK"
        private var sslConfig: ChatClientConfig.SslConfig = ChatClientConfig.SslConfig()
        private var context: Context? = null

        /**
         * Sets the application context.
         * This is required for proper user agent generation.
         */
        fun context(context: Context) = apply {
            this.context = context
            this.userAgent = generateUserAgent(context)
        }

        actual fun isDebug(debug: Boolean) = apply { this.isDebug = debug }

        actual fun userAgent(userAgent: String) = apply { this.userAgent = userAgent }

        actual fun sslConfig(block: ChatClientConfig.SslConfig.Builder.() -> Unit) = apply {
            this.sslConfig = ChatClientConfig.SslConfig.Builder().apply(block).build()
        }

        actual fun build(): ChatClientConfig.PlatformConfig {
            return ChatClientConfig.PlatformConfig(
                isDebug = isDebug,
                userAgent = userAgent,
                sslConfig = sslConfig
            )
        }

        private fun generateUserAgent(context: Context): String {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }

            return "Stream Chat Android SDK/$versionName ($versionCode) " +
                "Android/${Build.VERSION.RELEASE} " +
                "(${Build.MANUFACTURER} ${Build.MODEL}; " +
                "API ${Build.VERSION.SDK_INT})"
        }
    }
} 