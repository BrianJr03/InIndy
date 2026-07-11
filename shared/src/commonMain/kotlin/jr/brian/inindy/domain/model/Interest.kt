package jr.brian.inindy.domain.model

enum class Interest(val displayName: String) {
    // Outdoor & Active
    RUNNING("Running"),
    HIKING("Hiking"),
    CYCLING("Cycling"),
    WALKING("Walking"),
    YOGA("Yoga"),
    SPORTS("Sports"),
    SWIMMING("Swimming"),
    SKATING("Skating"),

    // Social & Casual
    PICNICS("Picnics"),
    BONFIRES("Bonfires"),
    GAME_NIGHTS("Game Nights"),
    COFFEE("Coffee"),
    FOOD("Food & Drinks"),
    VOLUNTEERING("Volunteering"),

    // Creative & Enrichment
    PHOTOGRAPHY("Photography"),
    DRAWING("Drawing & Sketching"),
    READING("Reading"),
    MUSIC("Music"),
    CRAFTS("Crafts"),
    WRITING("Writing"),

    // Exploration & Nature
    EXPLORING("Exploring"),
    BIRDWATCHING("Birdwatching"),
    GARDENING("Gardening"),
    STARGAZING("Stargazing"),
    NATURE("Nature Walks"),

    // Pets
    DOG_WALKS("Dog Walks"),
    PET_FRIENDLY("Pet Friendly");

    companion object {
        /**
         * Parses stored enum names (from the DB, DataStore prefs, or a network
         * DTO) into domain values, silently dropping any names that no longer
         * map to an [Interest]. This tolerates renames and removals — a stale
         * name won't crash the app on read.
         *
         * Returns a [List]; call sites that need a `Set` can add `.toSet()`.
         */
        fun fromStorageNames(names: Iterable<String>): List<Interest> =
            names.mapNotNull { name -> runCatching { valueOf(name) }.getOrNull() }
    }
}