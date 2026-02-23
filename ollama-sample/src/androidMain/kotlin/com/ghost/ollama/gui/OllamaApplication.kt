package com.ghost.ollama.gui

import android.app.Application
import com.ghost.ollama.gui.di.appModule
import com.ghost.ollama.gui.di.platformModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class OllamaApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@OllamaApplication)
            modules(appModule, platformModule)
        }
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }
    }
}