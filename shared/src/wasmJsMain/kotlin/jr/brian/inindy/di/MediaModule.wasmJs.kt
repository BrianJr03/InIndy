package jr.brian.inindy.di

import jr.brian.inindy.data.media.AppSettingsOpener
import jr.brian.inindy.data.media.CameraCapture
import jr.brian.inindy.data.media.ImageCompressor
import jr.brian.inindy.data.media.ImagePicker
import jr.brian.inindy.data.repository.FakeMediaRepository
import jr.brian.inindy.domain.repository.MediaRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val mediaModule: Module = module {
    single { CameraCapture() }
    single { ImagePicker() }
    single { ImageCompressor() }
    single { AppSettingsOpener() }
    // wasmJs has no supabase-kt artifact, so the fake repo remains here for now.
    single<MediaRepository> { FakeMediaRepository() }
}
