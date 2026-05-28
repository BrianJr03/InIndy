package jr.brian.inindy.domain.model

data class AttendanceRecord(
    val postId: String,
    val postTitle: String,
    val postImageUrl: String?,
    val hostName: String,
    val attendedAt: Long
)
