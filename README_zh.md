# ADB 文件管理器

*阅读：[English](README.md) | [中文](README_zh.md)*

一款使用 Kotlin 和 Compose Desktop 构建、通过 ADB 浏览和管理 Android 设备的桌面文件管理器。

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-blue.svg)
![Compose](https://img.shields.io/badge/Compose-1.9.0-green.svg)
![Version](https://img.shields.io/badge/version-v3.1.0-orange.svg)
![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)

## 主要功能

- **双栏文件管理器**：左侧浏览本地电脑，右侧浏览 Android 设备，并可双向复制文件。
- **可折叠本地栏**：可折叠为紧凑窄栏，恢复接近旧版的设备单栏体验；应用会记住选择。
- **可靠的 Windows 传输**：使用结构化进程参数，并通过纯 ASCII 暂存文件上传、Android 端重命名，解决旧代码页 Windows 上空格、中文、日文等文件名损坏的问题。
- **双向拖放**：支持两栏之间及应用与操作系统之间拖放。拖放只能从独立手柄发起，避免点击文件夹时误触复制。
- **可取消传输**：取消会终止传输协程与 ADB 子进程、复位界面状态，并清理由本次操作新建的残缺文件。
- **内置代码编辑器**：支持语法高亮、查找替换、撤销重做、编码切换、光标位置以及无损 UTF-8 保存。
- **ADB 终端**：支持多个交互式终端标签、命令模式切换，以及把本地文件拖入命令。
- **设备工具**：应用列表、APK 备份与卸载、截图、剪贴板、书签、搜索、排序、权限修改、重命名、复制、移动、删除和批量操作。
- **自适应桌面界面**：窗口尺寸根据屏幕动态计算，支持浅色、深色、跟随系统主题及中英文界面。

## 相比 v2.6.1 的变化

v3.1.0 将原来的设备单栏文件视图升级为双栏工作流，并加入完整的代码编辑器和终端体验。同时修复了 Windows CP936/GBK 环境中，文件名包含空格或非 ASCII 字符时上传失败、远程文件名截断或损坏的问题。

详细功能对比、修复内容和升级说明请查看 [v3.1.0 发布说明](RELEASE_NOTES_v3.1.0.md)。

## 运行要求

- Windows、macOS 或 Linux 桌面系统
- 已启用 ADB 调试的 Android 设备或模拟器
- 从源码构建时需要 Java 21

应用已经内置 ADB。Windows 启动时会先用 SHA-256 校验内置与本地运行时；内容一致或 `adb.exe` 正在运行时不会强行覆盖。

## 安装

请从 [GitHub Releases](https://github.com/wkbin/AdbFileManager/releases) 下载对应系统的安装包。Windows 用户可直接安装 `.msi` 文件。

### 从源码运行

```bash
git clone https://github.com/wkbin/AdbFileManager.git
cd AdbFileManager
./gradlew run
```

运行测试：

```bash
./gradlew test
```

构建当前系统的原生安装包：

```bash
# Windows
./gradlew packageMsi

# macOS
./gradlew packageDmg

# Linux
./gradlew packageDeb
```

## 使用方式

1. 在 Android 设备上启用 USB 调试，并通过 USB 或无线 ADB 连接。
2. 在 ADB 文件管理器中选择设备。
3. 在左栏浏览本地目录，在右栏浏览 Android 存储空间。
4. 点击文件行上的方向箭头复制到另一栏，或从文件行的拖拽手柄开始拖放。
5. 点击 Android 侧支持的文本文件，可直接打开并编辑。
6. 点击终端按钮可执行交互式 ADB 或设备 Shell 命令。

本地栏以导航和传输为核心，因此不会提供删除、重命名和权限修改等破坏性本地操作。

## Windows 使用提示

- 不要直接导出到 `C:\Users` 等受保护目录。请先进入自己的用户目录、下载、桌面或其他可写目录。
- 应用会在导出前检查目标目录；本地目录只读时会禁用向左复制、移动操作。
- 即使系统 ANSI 代码页为 GBK/CP936，也支持包含空格、中文、日文等字符的文件名。

## 技术栈

- Kotlin、Kotlin Coroutines 和 Flow
- Jetpack Compose for Desktop / Material 3
- Android Debug Bridge（ADB）
- Koin 依赖注入
- Mozilla Universal Charset Detector

## 贡献

欢迎贡献代码、报告问题或提出功能建议，详情请查看[贡献指南](CONTRIBUTING_zh.md)。

## 许可证

本项目基于 [MIT 许可证](LICENSE)发布。
