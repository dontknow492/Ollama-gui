# Ollama GUI (Kotlin Multiplatform)

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/yourusername/your-repo/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-blue)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Compose-Multiplatform-orange)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey)](./LICENSE)

A modern **Ollama Desktop & Android GUI** built with **Kotlin Multiplatform + Compose**.

Designed for local-first AI workflows, this application provides a clean, responsive interface to interact with Ollama models — supporting chat, streaming responses, model management, and persistent sessions.

---

## Overview

Ollama GUI is a cross-platform application that runs on:

* 🖥️ Windows (MSI)
* 🍎 macOS (DMG)
* 🐧 Linux (DEB)
* 📱 Android

It connects to a local or remote Ollama server and offers a fast, structured UI for model interaction, chat history, and generation workflows.

This is not just a client wrapper — it’s a full GUI experience built on a shared multiplatform architecture.

---

## Features

### Core AI Interaction

* Chat with Ollama models
* Real-time streaming responses
* Model switching
* Session-based conversations
* Markdown rendering with syntax highlighting
* Code block formatting

### Desktop Experience

* Native installers (MSI / DMG / DEB)
* System window integration
* Optimized runtime image (jlink)
* Persistent local database (SQLDelight)

### Android Experience

* Material 3 UI
* Adaptive layout
* Lifecycle-aware state management
* Paging support for chat history

### Architecture Highlights

* Kotlin Multiplatform (shared core logic)
* Compose Multiplatform UI
* Coroutines + Flow state management
* SQLDelight database
* Koin dependency injection
* Ktor networking layer

---

## Tech Stack

* Kotlin Multiplatform
* Compose Multiplatform
* Android SDK
* SQLDelight
* Ktor
* kotlinx.serialization
* Coroutines + Flow
* Koin
* Napier (logging)

---

## Project Structure

```
.
├── ollama-core          // Shared networking + business logic
├── ollama-gui           // Compose Multiplatform application
│   ├── androidMain
│   ├── desktopMain
│   └── commonMain
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

Architecture separation:

* `commonMain` → shared UI state, models, repository
* `androidMain` → Android-specific integrations
* `desktopMain` → Desktop runtime & packaging config

---

## Installation & Build

### Clone Repository

```bash
git clone https://github.com/dontknow492/Ollama-gui.git
cd Ollama-gui
```

---

## Run Desktop (Development)

```bash
./gradlew run
```

---

## Build Desktop Installer

### Windows

```bash
./gradlew packageMsi
```

### macOS

```bash
./gradlew packageDmg
```

### Linux

```bash
./gradlew packageDeb
```

Output location:

```
app/build/compose/binaries/main/
```

---

## Build Android

### Debug APK

```bash
./gradlew assembleDebug
```

### Release APK

```bash
./gradlew assembleRelease
```

### Play Store Bundle

```bash
./gradlew bundleRelease
```

Output location:

```
app/build/outputs/
```

---

## Configuration

By default, the app connects to:

```
http://localhost:11434
```

To change the endpoint:

* Modify base URL in configuration layer
* Or inject via environment config (recommended for production)

Future improvement: in-app server configuration UI.

---

## Screenshots

Add your screenshots in:

```
assets/screenshots/
```

Example:

```
assets/screenshots/desktop_chat.png
assets/screenshots/android_chat.png
```

---

## Roadmap

* Model pull / management UI
* File upload support
* Export chat history
* Theming system
* Multi-server configuration
* Plugin system
* Performance profiling mode
* Windows ARM support
* Auto-update mechanism

---

## Why This Project?

* Fully local AI workflow
* Cross-platform from one codebase
* Modern Compose UI
* Clean architecture
* Designed for extension

This project demonstrates production-ready Kotlin Multiplatform desktop engineering — not just a simple sample.

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Follow Kotlin + Compose conventions
4. Submit a PR to `main`

Feature proposals and architecture discussions are welcome.

---

## License

MIT License — see `LICENSE` for details.

---

## Acknowledgements

* Kotlin Multiplatform
* Compose Multiplatform
* SQLDelight
* Ktor
* kotlinx.serialization
* Ollama

---
