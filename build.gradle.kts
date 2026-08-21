import org.jetbrains.compose.desktop.application.dsl.TargetFormat
plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
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


dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs.compose)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.juniversalchardet)
    implementation(libs.okhttp)
    implementation(libs.apk.parser)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.navigation.compose)

    testImplementation(kotlin("test"))
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
            packageVersion = "3.1.2"
            description = "FileManager For Desktop"
            copyright = "Copyright 2023 Kebin Wang. All rights reserved."
            vendor = "Kebin Wang"
            modules(
                "java.base",
                "java.desktop",
                "java.prefs",
                "java.logging",
                "java.management",
                "java.naming",
                "java.sql",
                "java.xml",
                "jdk.unsupported",
                "jdk.crypto.ec"
            )

            // 自定义打包输出配置
            macOS {
                // macOS 特定配置
                packageName = "AdbFileManager-mac"
                // 不设置 dmgPackageVersion，避免重复添加版本号
            }

            windows {
                // Windows 特定配置
                packageName = "AdbFileManager-win"
                // 不设置 msiPackageVersion，避免重复添加版本号
                shortcut = true
                menu = true
                dirChooser = true
                perUserInstall = true
            }

            linux {
                // Linux 特定配置
                packageName = "AdbFileManager-linux"
                // 不设置 debPackageVersion，避免重复添加版本号
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
