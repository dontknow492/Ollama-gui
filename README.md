# Ollama GUI (Kotlin Multiplatform)

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/dontknow492/Ollama-gui/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-blue)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Compose-Multiplatform-orange)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey)](./LICENSE)

A modern **Desktop & Android GUI for Ollama**, built with **Kotlin Multiplatform + Compose**.

Designed for local-first AI workflows, this application provides a fast, clean, and privacy-focused interface to interact with Ollama models — supporting chat, streaming responses, model management, and persistent sessions.

---

# 🚀 What Is This?

Ollama GUI is a **cross-platform native application** that connects to an **Ollama server** and provides a full graphical interface for:

- Running LLM conversations
- Managing models
- Streaming responses in real time
- Persisting chat sessions locally

⚠️ **Important:**  
This application does not bundle Ollama internally.  
You must install and run Ollama separately.

The GUI communicates with the Ollama server over HTTP.

---

# 🖥 Supported Platforms

- 🪟 Windows (MSI installer)
- 🍎 macOS (DMG installer)
- 🐧 Linux (DEB package)
- 📱 Android

All powered by a shared Kotlin Multiplatform architecture.

---

# 🧠 Core Features

## AI Interaction

- Chat with any installed Ollama model
- Real-time streaming token output
- Markdown rendering with syntax highlighting
- Code block formatting
- Session-based conversation history
- Model switching per chat

## Model Management

- View installed models
- Download new models directly from inside the app
- Delete models
- Pull updates
- Refresh model list

## Desktop Experience

- Native OS installers (MSI / DMG / DEB)
- Optimized runtime image via `jlink`
- Window icon integration
- Local persistent database (SQLDelight)

## Android Experience

- Material 3 design
- Adaptive layouts
- Lifecycle-aware state management
- Paging support for chat history

---

# 🔐 Privacy & Local-First Design

This application is **privacy-focused**:

- No cloud dependency required
- No telemetry
- No data collection
- Chats stored locally in SQLDelight database
- Communicates only with your configured Ollama server

By default, it connects to:

```

[http://localhost:11434](http://localhost:11434)

```

You may configure a remote server if desired.

---

# 🧩 Architecture

```

.
├── ollama-core          // Shared networking + business logic
├── ollama-gui           // Compose Multiplatform application
│   ├── androidMain
│   ├── desktopMain
│   └── commonMain
├── build.gradle.kts
└── settings.gradle.kts

````

### Layer Breakdown

**commonMain**
- Shared UI state
- Repository layer
- ViewModels
- Models & DTOs

**androidMain**
- Android-specific integrations
- Lifecycle bindings
- Platform drivers

**desktopMain**
- Desktop runtime configuration
- Packaging configuration
- Window management

---

# 🛠 Ollama Setup (Required)

Before running this GUI, you must install Ollama.

## 1️⃣ Install Ollama

Visit:

👉 https://ollama.ai

Download and install for your OS.

---

## 2️⃣ Start Ollama Server

After installation, run:

```bash
ollama serve
````

This starts the server at:

```
http://localhost:11434
```

You can verify it's running:

```bash
curl http://localhost:11434/api/tags
```

---

## 3️⃣ Download a Model (CLI Method)

Example:

```bash
ollama pull llama3
```

Or download directly inside the GUI (recommended).

---

# 💻 Running The GUI

## Clone Repository

```bash
git clone https://github.com/dontknow492/Ollama-gui.git
cd Ollama-gui
```

---

## Run Desktop (Development Mode)

```bash
./gradlew run
```

---

## Build Desktop Installers

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

Output:

```
app/build/compose/binaries/main/
```

---

# 📱 Android Build

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

Output:

```
app/build/outputs/
```

---

# ⚙ Server Configuration

By default, the app connects to:

```
http://localhost:11434
```

You may:

* Modify base URL in configuration layer
* Inject environment-based config
* Future: Configure inside app settings UI

---

# 📦 Tech Stack

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

# 📸 Screenshots

Place screenshots in:

```
assets/screenshots/
```

Example:

```
assets/screenshots/desktop_chat.png
assets/screenshots/android_chat.png
```

---

# 🛣 Roadmap

* In-app server configuration screen
* Auto Ollama detection
* Embedded Ollama runtime (optional future)
* File upload support
* Chat export (Markdown / JSON)
* Multi-server support
* Theming system
* Plugin architecture
* Windows ARM support
* Auto-update mechanism

---

# 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Follow Kotlin + Compose conventions
4. Submit PR to `main`

Architecture discussions and feature proposals are welcome.

---

# 📜 License

MIT License — see `LICENSE`.

---

# 🙌 Acknowledgements

* Kotlin Multiplatform
* Compose Multiplatform
* SQLDelight
* Ktor
* kotlinx.serialization
* Ollama

---

# ⭐ Why This Project Matters

Most Ollama usage today is CLI or browser-based.

This project provides:

* Native desktop experience
* Local-first architecture
* Production-grade Kotlin Multiplatform design
* Clean, extensible codebase

Built for developers who want full control over their local AI workflows.
