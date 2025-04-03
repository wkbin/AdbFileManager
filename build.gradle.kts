import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
}

group = "top.wkbin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

// Configure source sets to exclude old UI files
kotlin {
    sourceSets {
        main {
            kotlin {
                exclude("**/ui/**")
            }
        }
    }
}

// Add compiler options for experimental OptIn
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
    }
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    implementation(compose.components.resources)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("com.darkrockstudios:mpfilepicker:3.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AdbFileManager"
            packageVersion = "2.0.3"
            description = "FileManager For Desktop"
            copyright = "Copyright 2023 Kebin Wang. All rights reserved."
            vendor = "Kebin Wang"
            modules("jdk.unsupported")
            windows{
                shortcut = true
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
