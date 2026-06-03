package jr.brian.inindy.di

import jr.brian.inindy.data.media.ActivityProvider
import jr.brian.inindy.data.media.AppSettingsOpener
import jr.brian.inindy.data.media.CameraCapture
import jr.brian.inindy.data.media.ImageCompressor
import jr.brian.inindy.data.media.ImagePicker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val mediaModule: Module = module {
    single { ActivityProvider() }
    single { CameraCapture(androidContext(), get()) }
    single { ImagePicker(get()) }
    single { ImageCompressor(androidContext()) }
    single { AppSettingsOpener(androidContext()) }
}
