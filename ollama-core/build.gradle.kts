plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
    kotlin("plugin.serialization") version "2.3.10"
}

kotlin {
    androidTarget()
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "com.ollama"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }
}

//mavenPublishing {
//    publishToMavenCentral()
//
//    signAllPublications()
//
//    coordinates(
//        groupId = "com.ghost",
//        artifactId = "ollama-kmp",
//        version = "0.1.0"
//    )
//
//    pom {
//        name.set("Ollama KMP")
//        description.set("Kotlin Multiplatform SDK for Ollama API")
//        inceptionYear.set("2026")
//        url.set("https://github.com/yourname/ollama-kmp")
//
//        licenses {
//            license {
//                name.set("MIT")
//                url.set("https://opensource.org/licenses/MIT")
//            }
//        }
//
//        developers {
//            developer {
//                id.set("yourid")
//                name.set("Your Name")
//            }
//        }
//
//        scm {
//            url.set("https://github.com/yourname/ollama-kmp")
//        }
//    }
//}