import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.gradle.download.task)
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
    implementation(compose.components.resources)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.mpfilepicker)
    implementation(libs.kotlinx.serialization)
}

tasks.register<Copy>("copyComposeResources") {
    from(layout.projectDirectory.dir("src/main/composeResources"))
    into(layout.buildDirectory.dir("copiedComposeResources"))
}

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadAdbZip") {
    dependsOn("copyComposeResources")

    // 下载地址可以在这个url找到 https://dl.google.com/android/repository/repository2-2.xml
    val windowsUrl = "platform-tools_r36.0.0-win.zip"
    val linuxUrl = "platform-tools_r36.0.0-linux.zip"
    val macOSUrl = "platform-tools_r36.0.0-darwin.zip"

    val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()

    val (url, targetDir) = when {
        os.isWindows -> windowsUrl to "windows"
        os.isLinux -> linuxUrl to "linux"
        os.isMacOsX -> macOSUrl to "macos"
        else -> throw GradleException("Unsupported System.")
    }

    src("https://dl.google.com/android/repository/$url")
    dest(layout.buildDirectory.file("copiedComposeResources/files/$targetDir/adb.zip"))
    overwrite(false)
}

afterEvaluate {
    tasks.getByName("convertXmlValueResourcesForMain").dependsOn("downloadAdbZip")
    tasks.getByName("copyNonXmlValueResourcesForMain").dependsOn("downloadAdbZip")
    tasks.getByName("prepareComposeResourcesTaskForMain").dependsOn("downloadAdbZip")
}

compose.resources {
    customDirectory(sourceSetName = "main", directoryProvider =
        layout.buildDirectory.dir("copiedComposeResources")
    )
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
            packageVersion = "2.4.1"
            description = "FileManager For Desktop"
            copyright = "Copyright 2023 Kebin Wang. All rights reserved."
            vendor = "Kebin Wang"
            modules("jdk.unsupported")

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
                dirChooser = true
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
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
