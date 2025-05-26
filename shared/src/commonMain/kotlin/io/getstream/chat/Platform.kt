package io.getstream.chat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform 