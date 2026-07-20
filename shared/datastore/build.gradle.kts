import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.shang.jetpackmoviekmp.shared.datastore"
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
            api(libs.androidx.datastore)
            api(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
    }
}

kover {
    reports {
        filters {
            includes {
                packages(
                    "com.shang.jetpackmoviekmp.datastore",
                    "com.shang.jetpackmoviekmp.datastore.di",
                    "com.shang.jetpackmoviekmp.datastore.provider",
                )
            }
        }
        verify {
            rule("datastore layer business logic minimum coverage") {
                minBound(80)
            }
        }
    }
}
