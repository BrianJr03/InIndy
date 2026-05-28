package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.AttendanceRecord

interface AttendanceRepository {
    suspend fun getAttendanceHistory(limit: Int = 5): Result<List<AttendanceRecord>>
    suspend fun getAttendanceRate(): Result<Float>
}
