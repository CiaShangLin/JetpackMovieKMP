import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kover)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    androidLibrary {
        namespace = "com.shang.jetpackmoviekmp.shared"
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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.koin.core)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.room.paging)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.coil.network.ktor3)
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

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
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
        // Kover 0.9.8 的 verify rule 沒有獨立的 per-rule filters，filters 是套在整個 reports{}
        // 底下、對 log/HTML/XML/verify 全部生效。這裡刻意把 shared 模組的 Kover 範圍收斂成只有
        // introduce-ktor-network-layer 這次新增的業務邏輯層（network.di/datasource/extension/provider），
        // 不含 network.model（DTO 樣板佔比過高，equals/hashCode/copy/componentN 大多用不到）
        // 以及 shared 模組其他跟本次無關的既有程式碼（Platform、ThemeMode、UserData 等）。
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
