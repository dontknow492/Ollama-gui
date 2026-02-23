package com.ghost.ollama.gui.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ghost.ollama.gui.OllamaDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {

    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = OllamaDatabase.Schema,
            context = androidContext(),
            name = "ollama.db"
        )
    }
}