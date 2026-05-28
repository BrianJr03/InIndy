package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.model.AttendanceRecord
import jr.brian.inindy.domain.repository.AttendanceRepository
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.delay

class FakeAttendanceRepository : AttendanceRepository {

    override suspend fun getAttendanceHistory(limit: Int): Result<List<AttendanceRecord>> {
        delay(SHORT_DELAY_MS)
        val now = currentTimeMillis()
        val day = 86_400_000L
        val records = listOf(
            AttendanceRecord(
                postId = "att-1",
                postTitle = "Sunset yoga at White River",
                postImageUrl = "https://images.unsplash.com/photo-1599901860904-17e6ed7083a0?w=600",
                hostName = "Priya K.",
                attendedAt = now - 3 * day
            ),
            AttendanceRecord(
                postId = "att-2",
                postTitle = "Pickup soccer at Garfield",
                postImageUrl = null,
                hostName = "Marcus T.",
                attendedAt = now - 7 * day
            ),
            AttendanceRecord(
                postId = "att-3",
                postTitle = "Saturday Monon run",
                postImageUrl = "https://www.railstotrails.org/nitropack_static/pVKvLDLqSrRUaEyiNwEcSJukRyhzZaDI/assets/images/optimized/rev-958f862/www.railstotrails.org/wp-content/uploads/2024/12/Indianas-Monon-Trail_IMG_8344_Photo-by-Robert-Annis.jpg",
                hostName = "Jordan O.",
                attendedAt = now - 12 * day
            ),
            AttendanceRecord(
                postId = "att-4",
                postTitle = "Eagle Creek dawn hike",
                postImageUrl = "https://www.visitindy.com/imager/files_idss_com/C516/DMS_image_3410_e7b4e5d5-5056-854c-b6c0e14aadaa42c5_e45adf5f6bc0c5c2a30a39868f44eab6.jpg",
                hostName = "Audrea W.",
                attendedAt = now - 18 * day
            ),
            AttendanceRecord(
                postId = "att-5",
                postTitle = "Broad Ripple wine walk",
                postImageUrl = "https://wineandwalk.hr/wp-content/uploads/2024/09/wine_walk-00636.jpg",
                hostName = "Sam B.",
                attendedAt = now - 24 * day
            )
        )
        return Result.success(records.take(limit))
    }

    override suspend fun getAttendanceRate(): Result<Float> {
        delay(SHORT_DELAY_MS)
        return Result.success(0.92f)
    }

    private companion object {
        const val SHORT_DELAY_MS = 150L
    }
}
