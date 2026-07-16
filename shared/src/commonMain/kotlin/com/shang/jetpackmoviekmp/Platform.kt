package com.shang.jetpackmoviekmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
