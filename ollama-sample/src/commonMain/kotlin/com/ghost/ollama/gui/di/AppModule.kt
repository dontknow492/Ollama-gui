package com.ghost.ollama.gui.di

import com.ghost.ollama.OllamaClient
import com.ghost.ollama.createHttpClient
import com.ghost.ollama.gui.Message
import com.ghost.ollama.gui.OllamaDatabase
import com.ghost.ollama.gui.repository.OllamaRepository
import com.ghost.ollama.gui.ui.viewmodel.ChatViewModel
import com.ghost.ollama.gui.ui.viewmodel.SessionViewModel
import com.ghost.ollama.gui.utils.listOfStringAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module


val appModule = module {

    single {
        OllamaDatabase(
            driver = get(),
            MessageAdapter = Message.Adapter(
                imagesAdapter = listOfStringAdapter
            )
        )
    }

    single { get<OllamaDatabase>().entityQueries }

    // HTTP Client
    single {
        createHttpClient() // Make sure this function is in scope
    }

    single {
        OllamaClient(
            httpClient = createHttpClient()
        )
    }
    factory {
        OllamaRepository(
            ollamaClient = get(),
            entityQueries = get(),
            ioDispatcher = get()
        )
    }

    factory { params ->
        ChatViewModel(
            ollamaRepository = get()  // Make sure parameter name matches constructor
        )
    }

    factory { params ->
        SessionViewModel(
            repository = get()  // Or whatever parameter name your SessionViewModel uses
        )
    }

    // Dispatcher

    factory {
        SessionViewModel(get())
    }

    single<CoroutineDispatcher> { Dispatchers.IO }
}

expect val platformModule: Module