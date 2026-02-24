import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":ollama-core"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)


                // sqldelight
//                implementation(libs.sqldelight.primitive.adaptor)
                implementation(libs.sqldelight.androidx.paging3.extension)
                implementation(libs.sqldelight.coroutines.extensions)

                // datetime
                implementation(libs.kotlinx.datetime)

                // koin
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewModel)

                //logging
                implementation(libs.napier)

//                paging
                implementation(libs.cash.paging.runtime)
                implementation(libs.cash.paging.compose)


//                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")

                // markdown
                implementation(libs.kmp.markdown.render)
                implementation(libs.kmp.markdown.render.m3)
                implementation(libs.markdow.code.highlights)
                implementation(libs.highlights)


            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.core.ktx)

                implementation(libs.sqldelight.android.driver)


                implementation(libs.koin.android)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.jvm.driver)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

android {
    namespace = "com.ghost.ollama.gui"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ghost.ollama.gui"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    // Set JVM toolchain to Java 11
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

//    kotlinOptions {
//        jvmTarget = "11"
//    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}


dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.ghost.ollama.gui.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "OllamaSample"
            packageVersion = "1.0.0"
        }
    }
}


sqldelight {
    databases {
        create("OllamaDatabase") {
            packageName.set("com.ghost.ollama.gui")
            srcDirs("src/sqldelight/")
        }
    }
}