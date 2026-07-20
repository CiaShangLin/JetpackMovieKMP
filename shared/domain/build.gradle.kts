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
        namespace = "com.shang.jetpackmoviekmp.shared.domain"
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
            api(projects.shared.data)
            api(libs.androidx.paging.common)
            implementation(libs.koin.core)
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
                    "com.shang.jetpackmoviekmp.domain.usecase",
                    "com.shang.jetpackmoviekmp.domain.di",
                )
            }
        }
        verify {
            rule("domain layer business logic minimum coverage") {
                minBound(80)
            }
        }
    }
}
