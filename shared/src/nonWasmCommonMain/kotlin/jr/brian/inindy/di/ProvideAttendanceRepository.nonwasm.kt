package jr.brian.inindy.di

import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.repository.SupabaseAttendanceRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.AttendanceRepository

actual fun provideAttendanceRepository(
    currentUserProvider: CurrentUserProvider
): AttendanceRepository = SupabaseAttendanceRepository(
    supabase = SupabaseClientProvider.client,
    currentUserProvider = currentUserProvider
)
