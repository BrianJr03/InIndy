package jr.brian.inindy.di

import jr.brian.inindy.data.media.AppSettingsOpener
import jr.brian.inindy.data.media.CameraCapture
import jr.brian.inindy.data.media.ImageCompressor
import jr.brian.inindy.data.media.ImagePicker
import jr.brian.inindy.data.remote.SupabaseClientProvider
import jr.brian.inindy.data.remote.media.MediaRemoteDataSource
import jr.brian.inindy.data.remote.media.MediaRemoteDataSourceImpl
import jr.brian.inindy.data.repository.MediaRepositoryImpl
import jr.brian.inindy.domain.repository.MediaRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val mediaModule: Module = module {
    single { CameraCapture() }
    single { ImagePicker() }
    single { ImageCompressor() }
    single { AppSettingsOpener() }
    single<MediaRemoteDataSource> {
        MediaRemoteDataSourceImpl(
            supabase = SupabaseClientProvider.client,
            httpClient = get()
        )
    }
    single<MediaRepository> {
        MediaRepositoryImpl(
            remoteDataSource = get(),
            imageCompressor = get()
        )
    }
}
