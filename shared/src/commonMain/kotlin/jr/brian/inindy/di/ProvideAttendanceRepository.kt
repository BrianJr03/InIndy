package jr.brian.inindy.di

import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.AttendanceRepository

/**
 * Returns the active AttendanceRepository for this platform.
 * On Android/iOS/JS this is SupabaseAttendanceRepository.
 * On wasmJs this falls back to FakeAttendanceRepository (supabase-kt has no wasmJs artifact).
 */
expect fun provideAttendanceRepository(
    currentUserProvider: CurrentUserProvider
): AttendanceRepository
