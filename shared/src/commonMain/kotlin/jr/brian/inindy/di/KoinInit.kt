package jr.brian.inindy.di

import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    if (GlobalContext.getOrNull() != null) return
    startKoin {
        appDeclaration()
        modules(appModules)
    }
}

fun initKoinIos() = initKoin()
