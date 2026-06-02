package jr.brian.inindy.domain.model

sealed class ExploreFilter {
    data object All : ExploreFilter()
    data object Neighborhood : ExploreFilter()
    data class Group(
        val groupId: String,
        val groupName: String
    ) : ExploreFilter()
}

fun ExploreFilter.toBrandMarkText(neighborhoodName: String): String =
    when (this) {
        is ExploreFilter.All -> "InIndy"
        is ExploreFilter.Neighborhood -> "In${neighborhoodName.toFilterLabel()}"
        is ExploreFilter.Group -> "In${groupName.toFilterLabel()}"
    }

fun String.toFilterLabel(): String =
    this.split(" ")
        .filter { it.isNotBlank() }
        .joinToString("") { word ->
            word.filter { it.isLetterOrDigit() }
                .replaceFirstChar { it.uppercaseChar() }
        }
