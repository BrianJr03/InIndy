package jr.brian.inindy.domain.model

data class User(
    val id: String,
    val fullName: String?,
    val avatarUrl: String?,
    val phoneVerified: Boolean = false,
    val neighborhoodId: String? = null,
    val interests: List<Interest> = emptyList()
)

val User.isOnboardingComplete: Boolean
    get() = fullName != null && neighborhoodId != null && interests.isNotEmpty()
