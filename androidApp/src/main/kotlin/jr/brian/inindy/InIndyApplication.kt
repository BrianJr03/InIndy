package jr.brian.inindy

import android.app.Application
import jr.brian.inindy.di.initKoin
import org.koin.android.ext.koin.androidContext

class InIndyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@InIndyApplication)
        }
    }
}
