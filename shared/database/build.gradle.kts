import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kover)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.shang.jetpackmoviekmp.shared.database"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.common)
            api(projects.shared.model)
            api(libs.androidx.room.runtime)
            implementation(libs.androidx.room.paging)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
        iosTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kover {
    reports {
        filters {
            includes {
                packages(
                    "com.shang.jetpackmoviekmp.database",
                    "com.shang.jetpackmoviekmp.database.di",
                    "com.shang.jetpackmoviekmp.database.entity",
                )
            }
            excludes {
                packages("com.shang.jetpackmoviekmp.database.dao")
                classes("com.shang.jetpackmoviekmp.database.DatabaseBuilder_androidKt*")
                classes("com.shang.jetpackmoviekmp.database.AppDatabase_Impl*")
                classes("com.shang.jetpackmoviekmp.database.AppDatabaseConstructor*")
            }
        }
        verify {
            rule("database layer business logic minimum coverage") {
                minBound(80)
            }
        }
    }
}
