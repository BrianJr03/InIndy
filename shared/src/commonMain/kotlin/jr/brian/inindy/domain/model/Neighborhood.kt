package jr.brian.inindy.domain.model

data class Neighborhood(
    val id: String,
    val name: String,
    val city: String = "Indianapolis",
    val slug: String
)
