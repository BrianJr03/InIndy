package jr.brian.inindy.di

import jr.brian.inindy.util.initAppLogging
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return
    initAppLogging()
    startKoin {
        appDeclaration()
        modules(appModules)
    }
}

fun initKoinIos() = initKoin()
