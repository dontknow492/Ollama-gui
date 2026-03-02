creaet readme.md for this as fix
Below is a ready\-to\-use `README.md` for the Kotlin Multiplatform Ollama SDK project. It includes description, features, directory layout, tech stack, installation and usage examples, screenshots section, links, and a thanks\-to section.

```markdown
# Ollama KMP SDK

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/yourusername/your-repo/actions) [![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-blue)](https://kotlinlang.org/) [![License](https://img.shields.io/badge/license-MIT-lightgrey)](./LICENSE)

A Kotlin Multiplatform SDK for interacting with Ollama models. Designed to work on Kotlin/JVM, Kotlin/Native (macOS, Linux), and Kotlin/JS targets — providing a common, idiomatic Kotlin API for requests, streaming, and serialization.

## Table of contents

- [Description](#description)
- [Features](#features)
- [Tech stack](#tech-stack)
- [Basic directory structure](#basic-directory-structure)
- [Installation](#installation)
- [Quick usage](#quick-usage)
- [Configuration](#configuration)
- [Screenshots](#screenshots)
- [Contributing](#contributing)
- [License](#license)
- [Thanks](#thanks)

## Description

Ollama KMP SDK provides a lightweight, multiplatform client to call Ollama models from Kotlin code. It centralizes networking, JSON serialization, streaming helpers, and multiplatform wiring so applications can use Ollama models on Android, desktop, server, and JS/Browser environments.

## Features

- Kotlin Multiplatform (common API for JVM/Native/JS)
- Simple, coroutine\-friendly request API
- Streaming responses support
- Configurable HTTP client backends per platform
- Typed requests / responses with `kotlinx.serialization`
- Small surface area for easy integration into apps and libraries

## Tech stack

- Kotlin Multiplatform (KMP)
- Gradle (Kotlin DSL) and Ant (if needed in CI)
- kotlinx\-serialization
- kotlinx\-coroutines
- ktor (or platform HTTP client adapters)
- GitHub Actions (CI)

Useful links:
- Kotlin: https://kotlinlang.org/
- Gradle: https://gradle.org/
- kotlinx\-serialization: https://github.com/Kotlin/kotlinx.serialization
- ktor: https://ktor.io/

## Basic directory structure

Example layout (KMP convention):

```
.
├── build.gradle.kts
├── settings.gradle.kts
├── gradle
├── src
│   ├── commonMain
│   │   └── kotlin
│   │       └── com
│   │           └── example
│   │               └── ollama
│   │                   ├── OllamaClient.kt
│   │                   └── models.kt
│   ├── jvmMain
│   ├── linuxX64Main
│   └── jsMain
├── README.md
└── LICENSE
```

## Installation

Add the SDK to your multiplatform project. Example using Gradle Kotlin DSL:

1. Add repository (if publishing to MavenCentral/GitHub Packages, use that repo):

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.pkg.github.com/yourusername/your-repo") // if using GitHub Packages
}
```

2. Add dependency to your KMP targets (use the real group/artifact/version from your published package):

```kotlin
kotlin {
    // configure targets...
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.yourorg:ollama-kmp-sdk:0.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.0")
                implementation("io.ktor:ktor-client-core:2.3.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
            }
        }
    }
}
```

Platform specific HTTP client engines (add to jvmMain, jsMain, etc.):

```kotlin
// JVM
implementation("io.ktor:ktor-client-cio:2.3.0")
// JS
implementation("io.ktor:ktor-client-js:2.3.0")
```

## Quick usage

Below is a minimal example showing how to instantiate the client and make a request. Adjust to your SDK's actual API.

```kotlin
import kotlinx.coroutines.runBlocking
import com.yourorg.ollama.OllamaClient
import com.yourorg.ollama.models.OllamaRequest

fun main() = runBlocking {
    val client = OllamaClient {
        baseUrl = "http://localhost:11434" // or remote Ollama endpoint
        apiKey = null // set if required
    }

    val request = OllamaRequest(
        model = "llama2",
        prompt = "Write a short Kotlin function that returns the fibonacci sequence up to n."
    )

    val response = client.generate(request)
    println("Response: ${response.output}")
}
```

Streaming example (coroutine flow):

```kotlin
client.streamGenerate(request).collect { chunk ->
    print(chunk.text)
}
```

## Configuration

- `baseUrl` — endpoint for the Ollama service (defaults to `http://localhost:11434`)
- `timeout` — request timeout configuration per platform
- `httpClientFactory` — inject platform HTTP client if you need custom config

(Check the SDK docs in `docs/` or KDoc for full config API.)

## Screenshots

Add runtime screenshots or examples here. Example:

![Example usage screenshot](assets/screenshot-usage.png)

If you publish screenshots, place them in the repository under `assets/` or `docs/assets/` and reference them using the relative path.

## Contributing

Contributions are welcome. Typical workflow:

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/my-feature`
3. Run tests and linters
4. Open a pull request against `main`

Please follow the project's coding conventions and include tests for new features.

## License

This project is licensed under the MIT License. See `LICENSE` for details.

## Thanks

Thanks to the following projects and libraries used in this SDK:

- kotlinx\-serialization — https://github.com/Kotlin/kotlinx.serialization
- kotlinx\-coroutines — https://github.com/Kotlin/kotlinx.coroutines
- ktor — https://github.com/ktorio/ktor
- Kotlin Multiplatform community — https://kotlinlang.org/docs/multiplatform.html
- Ollama (models/service) — https://ollama.ai/

If you used any other third\-party components, add them here with links and attribution.

---

If you want, replace the placeholder URLs, versions, and examples with the real artifact coordinates and live API paths before publishing.
```