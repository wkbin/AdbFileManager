# ADB File Manager

*Read this in [English](README.md) | [中文](README_zh.md)*

A desktop file manager for browsing and managing Android devices through ADB, built with Kotlin and Compose Desktop.

![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-blue.svg)
![Compose](https://img.shields.io/badge/Compose-1.8.0-green.svg)
![Version](https://img.shields.io/badge/version-v3.1.0-orange.svg)
![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)

## Highlights

- **Dual-pane file manager** — browse the local computer on the left and the Android device on the right, then copy files in either direction.
- **Collapsible local pane** — collapse the local pane for a compact, device-focused layout. The preference is remembered between launches.
- **Reliable Windows transfers** — paths are passed as structured process arguments, and uploads use an ASCII staging file plus an Android-side rename to preserve spaces, Chinese, Japanese, and other Unicode names on legacy Windows code pages.
- **Drag and drop** — drag files between the local and Android panes or between the application and the operating system. Dedicated drag handles prevent accidental transfers when opening folders.
- **Cancellable transfers** — cancelling a transfer stops its coroutine and ADB child process, resets the UI, and removes newly created incomplete files.
- **Built-in code editor** — edit remote text files with syntax highlighting, find/replace, undo/redo, encoding selection, cursor position, and lossless UTF-8 saving.
- **ADB terminal** — open multiple interactive terminal tabs, run commands, switch shell modes, and drop local files into commands.
- **Device tools** — application list, APK backup and uninstall, screenshots, clipboard transfer, bookmarks, search, sorting, permissions, rename, copy, move, delete, and batch operations.
- **Adaptive desktop UI** — responsive initial window sizing, light/dark/system themes, and Chinese/English interfaces.

## Changes since v2.6.1

Version 3.1.0 replaces the former device-only file view with a dual-pane workflow and adds a full editor and terminal experience. It also fixes the Windows CP936/GBK transfer failures that affected filenames containing spaces or non-ASCII characters.

See [Release notes for v3.1.0](RELEASE_NOTES_v3.1.0.md) for the detailed comparison, fixes, and upgrade notes.

## Requirements

- Windows, macOS, or Linux desktop
- An Android device or emulator with ADB debugging enabled
- Java 21 when building from source

ADB is bundled with the application. On Windows, the bundled runtime is verified with SHA-256 before use, so an identical or currently running `adb.exe` is not overwritten during startup.

## Install

Download the package for your operating system from the [GitHub Releases](https://github.com/wkbin/AdbFileManager/releases) page. Windows users can install the `.msi` package directly.

### Build from source

```bash
git clone https://github.com/wkbin/AdbFileManager.git
cd AdbFileManager
./gradlew run
```

Run tests:

```bash
./gradlew test
```

Build a native installer for the current operating system:

```bash
# Windows
./gradlew packageMsi

# macOS
./gradlew packageDmg

# Linux
./gradlew packageDeb
```

## Usage

1. Enable USB debugging on the Android device and connect it by USB or wireless ADB.
2. Select the device in ADB File Manager.
3. Browse local folders in the left pane and Android storage in the right pane.
4. Use the arrow action on a row to copy it to the opposite pane, or drag from the row's drag handle.
5. Open supported text files on the Android side to edit them directly.
6. Use the terminal button for interactive ADB or device-shell commands.

The local pane intentionally focuses on navigation and transfer. Destructive local operations such as delete, rename, and permission changes are not exposed there.

## Notes for Windows

- Avoid exporting into protected folders such as `C:\Users` itself. Open your own user directory, Downloads, Desktop, or another writable folder first.
- The application checks the destination before export and disables opposite-pane copy actions for read-only local folders.
- Unicode and spaced filenames are supported even when the Windows ANSI code page is GBK/CP936.

## Technology

- Kotlin and Kotlin Coroutines/Flow
- Jetpack Compose for Desktop / Material 3
- Android Debug Bridge (ADB)
- Koin dependency injection
- Mozilla Universal Charset Detector

## Contributing

Contributions, issue reports, and feature suggestions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Star History

[![Star History Chart](https://starchart.cc/wkbin/AdbFileManager.svg)](https://starchart.cc/wkbin/AdbFileManager)

## License

Released under the [MIT License](LICENSE).
