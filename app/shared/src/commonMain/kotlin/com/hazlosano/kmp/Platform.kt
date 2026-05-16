package com.hazlosano.kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform