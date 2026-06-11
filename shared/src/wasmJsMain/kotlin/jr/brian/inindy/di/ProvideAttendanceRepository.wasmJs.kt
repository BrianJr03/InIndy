package jr.brian.inindy.di

import jr.brian.inindy.data.repository.FakeAttendanceRepository
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.AttendanceRepository

// supabase-kt has no wasmJs artifact, so this target keeps the fake.
@Suppress("UNUSED_PARAMETER")
actual fun provideAttendanceRepository(
    currentUserProvider: CurrentUserProvider
): AttendanceRepository = FakeAttendanceRepository()
