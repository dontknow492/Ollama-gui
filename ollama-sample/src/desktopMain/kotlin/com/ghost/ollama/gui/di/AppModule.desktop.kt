package com.ghost.ollama.gui.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ghost.ollama.gui.OllamaDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {

    // SQLDelight driver
    single<SqlDriver> {
        val driver = JdbcSqliteDriver("jdbc:sqlite:ollama.db")




        try {
            // Try to create schema - will fail if tables exist
            OllamaDatabase.Schema.create(driver)
            println("✅ Database schema created")
        } catch (e: Exception) {
            // If tables already exist, just log and continue
            println("📁 Database schema already exists (or error: ${e.message})")
            // You might want to check if it's actually a "table already exists" error
        }

        driver
    }

}