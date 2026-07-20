import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.shang.jetpackmoviekmp.shared.network"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.common)
            api(projects.shared.model)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.koin.test)
        }
    }
}

val keyProperties = Properties().apply {
    val keyPropertiesFile = rootProject.file("key.properties")
    if (keyPropertiesFile.exists()) {
        keyPropertiesFile.inputStream().use { load(it) }
    }
}

buildConfig {
    packageName("com.shang.jetpackmoviekmp")
    buildConfigField("TMDB_API_KEY", keyProperties.getProperty("TMDB_API_KEY", ""))
}

kover {
    reports {
        filters {
            includes {
                packages(
                    "com.shang.jetpackmoviekmp.network.di",
                    "com.shang.jetpackmoviekmp.network.datasource",
                    "com.shang.jetpackmoviekmp.network.extension",
                    "com.shang.jetpackmoviekmp.network.provider",
                )
            }
        }
        verify {
            rule("network layer business logic minimum coverage") {
                minBound(80)
            }
        }
    }
}
