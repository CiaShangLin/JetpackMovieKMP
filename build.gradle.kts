import org.gradle.api.attributes.Bundling

plugins {
    base

    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(libs.ktlint.cli) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val ktlintArgs = listOf(
    "**/src/**/*.kt",
    "**/*.gradle.kts",
    "!**/build/**",
    "!**/.gradle/**",
)

val ktlintCheck = tasks.register<JavaExec>("ktlintCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"

    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(ktlintArgs)
}

val ktlintFormat = tasks.register<JavaExec>("ktlintFormat") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style and format"

    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args("-F")
    args(ktlintArgs)
}

ktlintCheck {
    mustRunAfter(ktlintFormat)
}

tasks.named("check") {
    dependsOn(ktlintFormat, ktlintCheck)
}

subprojects {
    tasks.matching { task ->
        task.name == "preBuild" || task.name == "androidPreBuild"
    }.configureEach {
        dependsOn(ktlintFormat, ktlintCheck)
    }
}

fun toolCommand(command: String, installHint: String, vararg args: String): List<String> {
    val missingMessage = "Missing required command `$command`. $installHint"
    val commandWithArgs = listOf(command, *args).joinToString(" ")

    return if (System.getProperty("os.name").lowercase().contains("windows")) {
        listOf(
            "cmd",
            "/c",
            "where $command >NUL 2>NUL || (echo $missingMessage && exit /b 1) && $commandWithArgs",
        )
    } else {
        listOf(
            "sh",
            "-c",
            "command -v $command >/dev/null 2>&1 || { echo '$missingMessage'; exit 1; }; $commandWithArgs",
        )
    }
}

val iosSwiftSourcePath = "iosApp/iosApp"

tasks.register<Exec>("iosFormat") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Format iOS Swift code with SwiftFormat"

    workingDir = rootDir
    commandLine(
        toolCommand(
            "swiftformat",
            "Install SwiftFormat: brew install swiftformat",
            iosSwiftSourcePath,
            "--config",
            ".swiftformat",
        ),
    )
}

tasks.register<Exec>("iosFormatCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check iOS Swift formatting with SwiftFormat"

    workingDir = rootDir
    commandLine(
        toolCommand(
            "swiftformat",
            "Install SwiftFormat: brew install swiftformat",
            "--lint",
            iosSwiftSourcePath,
            "--config",
            ".swiftformat",
        ),
    )
}

tasks.register<Exec>("iosLint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Lint iOS Swift code with SwiftLint"

    workingDir = rootDir
    commandLine(
        toolCommand(
            "swiftlint",
            "Install SwiftLint: brew install swiftlint",
            "lint",
            "--config",
            ".swiftlint.yml",
        ),
    )
}

tasks.register("iosCodeStyleCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check iOS Swift formatting and lint"

    dependsOn("iosFormatCheck", "iosLint")
}
