import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(projects.shared.common)
            export(projects.shared.model)
            export(projects.shared.datastore)
            export(projects.shared.network)
            export(projects.shared.database)
            export(projects.shared.data)
            export(projects.shared.domain)
        }
    }

    androidLibrary {
        namespace = "com.shang.jetpackmoviekmp.shared.app"
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
            api(projects.shared.datastore)
            api(projects.shared.network)
            api(projects.shared.database)
            api(projects.shared.data)
            api(projects.shared.domain)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
    }
}
