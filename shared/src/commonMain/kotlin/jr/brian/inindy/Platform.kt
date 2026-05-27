package jr.brian.inindy

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform