package jr.brian.inindy.presentation.me

import jr.brian.inindy.domain.model.AttendanceRecord
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User

data class MeUiState(
    val user: User? = null,
    val neighborhoodName: String = "Broad Ripple",
    val recentPosts: List<Post> = emptyList(),
    val groups: List<Group> = emptyList(),
    val attendanceHistory: List<AttendanceRecord> = emptyList(),
    val attendanceRate: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null
)
