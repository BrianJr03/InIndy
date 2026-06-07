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
    PET_FRIENDLY("Pet Friendly"),
}