import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kover)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.shang.jetpackmoviekmp.shared.data"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.model)
            api(libs.androidx.paging.common)
            implementation(projects.shared.common)
            implementation(projects.shared.network)
            implementation(projects.shared.datastore)
            implementation(projects.shared.database)
            implementation(libs.coil.network.ktor3)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.koin.test)
        }
    }
}

kover {
    reports {
        filters {
            includes {
                packages(
                    "com.shang.jetpackmoviekmp.data.repository",
                    "com.shang.jetpackmoviekmp.data.paging",
                    "com.shang.jetpackmoviekmp.data.model",
                    "com.shang.jetpackmoviekmp.data.di",
                )
            }
        }
        verify {
            rule("data layer business logic minimum coverage") {
                minBound(80)
            }
        }
    }
}
