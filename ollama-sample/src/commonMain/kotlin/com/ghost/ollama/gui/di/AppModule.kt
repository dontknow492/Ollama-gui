package com.ghost.ollama.gui.di

import com.ghost.ollama.OllamaClient
import com.ghost.ollama.createHttpClient
import com.ghost.ollama.gui.Message
import com.ghost.ollama.gui.OllamaDatabase
import com.ghost.ollama.gui.repository.OllamaRepository
import com.ghost.ollama.gui.repository.SettingsRepository
import com.ghost.ollama.gui.ui.viewmodel.ChatViewModel
import com.ghost.ollama.gui.ui.viewmodel.GlobalSettingsViewModel
import com.ghost.ollama.gui.ui.viewmodel.SessionViewModel
import com.ghost.ollama.gui.utils.listOfStringAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module


val applicationScope = CoroutineScope(
    SupervisorJob() + Dispatchers.IO
)

// Platform-specific module (expect/actual)
expect val platformModule: Module

// Database module
val databaseModule = module {
    single {
        OllamaDatabase(
            driver = get(), // SQLDriver from platform module
            MessageAdapter = Message.Adapter(
                imagesAdapter = listOfStringAdapter
            )
        )
    }

    single { get<OllamaDatabase>().entityQueries }
}

// Network module
val networkModule = module {
    single { createHttpClient() }
    single { OllamaClient(httpClient = get()) }
}

// Repository module
val repositoryModule = module {
    single<CoroutineDispatcher> { Dispatchers.IO }

    factory {
        OllamaRepository(
            ollamaClient = get(),
            entityQueries = get(),
            ioDispatcher = get()
        )
    }

    single { SettingsRepository(get(), applicationScope) } // DataStore from platform module
}

// ViewModel module
val viewModelModule = module {
    factory { params ->
        ChatViewModel(
            ollamaRepository = get(),
            ioDispatcher = get()
        )
    }

    factory { SessionViewModel(get()) }

    factory { GlobalSettingsViewModel(get()) }


}

// Combine all modules
val appModule = module {
    includes(
        platformModule,      // Platform-specific (SQLDriver, DataStore)
        databaseModule,      // Database setup
        networkModule,       // HTTP client and API
        repositoryModule,    // Repositories
        viewModelModule      // ViewModels
    )
}