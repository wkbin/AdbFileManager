# ADB File Manager

*Read this in [English](#English) | [中文](#Chinese)*

<a name="English"></a>

## English

A Material You designed Android device file manager that enables remote file operations via ADB connection.

![Kotlin](https://img.shields.io/badge/kotlin-1.9.21-blue.svg)
![Compose](https://img.shields.io/badge/compose-1.5.11-green.svg)

### 📖 Introduction

ADB File Manager is a desktop application that allows you to remotely browse and manage Android device file systems via ADB connection. Built with a modern Material You interface using Jetpack Compose, it offers a smooth user experience and elegant animation effects.

### 📸 Preview

<div align="center">
  <img src="docs/images/1.png" alt="ADB File Manager Preview" width="800">
</div>

### ✨ Features

- 🎨 **Material You Design** - Modern UI design following Google's latest design language
- 📱 **Multi-device Support** - Connect to and manage multiple Android devices
- 📂 **Intuitive File Navigation** - File browser with path navigation bar, supporting path jump by clicking
- 📝 **File Editing** - Built-in text editor for editing text files on the device
- 🔒 **Permission Check** - Smart detection and prompting for file operation permission issues
- 🔄 **Real-time Feedback** - Real-time status feedback for all operations
- 📥 **File Transfer** - Support for downloading files from and uploading files to devices

### 🔧 Technology Stack

- **Kotlin** - Primary development language
- **Jetpack Compose** - Modern UI toolkit
- **ADB** - Android Debug Bridge tool
- **Coroutines** - Handling asynchronous operations
- **Flow** - Reactive data streams

### 🚀 Installation

#### Building from Source

1. Clone the repository
   ```bash
   git clone https://github.com/AdbFileManager.git
   cd AdbFileManager
   ```

2. Build with Gradle
   ```bash
   ./gradlew build
   ```

3. Run the application
   ```bash
   ./gradlew run
   ```

### 💡 Usage Instructions

1. **Connect a device**:
   - Ensure USB debugging is enabled on your device
   - Connect the device to your computer
   - Select your device from the dropdown menu

2. **Browse files**:
   - Click on folders to enter them
   - Use the path navigation bar to quickly jump to parent directories
   - Click the refresh button to update the file list

3. **File operations**:
   - Click the menu button next to a file to see available actions
   - Edit: Modify text file content
   - Download: Save the file to your local computer
   - Delete: Remove the file from the device

4. **Create folder**:
   - Click the "New Folder" button in the toolbar
   - Enter a folder name and confirm

### 📋 Upcoming Features

- [ ] File search functionality
- [ ] File permission modification
- [ ] Dark/light theme toggle
- [ ] Drag and drop file upload
- [ ] File preview functionality

### 🤝 Contributing

Contributions, issue reports, and feature suggestions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

1. Fork this repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Submit a Pull Request

### 📄 License

This project is released under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<a name="Chinese"></a>

## 中文

一个基于Material You设计的Android设备文件管理器，通过ADB连接实现远程文件操作。

![Kotlin](https://img.shields.io/badge/kotlin-1.9.21-blue.svg)
![Compose](https://img.shields.io/badge/compose-1.5.11-green.svg)

### 📖 简介

ADB File Manager是一款桌面应用程序，允许你通过ADB连接远程浏览和管理Android设备文件系统。采用Jetpack Compose构建的现代Material You界面，提供流畅的用户体验和优雅的动画效果。

### 📸 预览

<div align="center">
  <img src="docs/images/1.png" alt="ADB文件管理器预览" width="800">
</div>

### ✨ 特点

- 🎨 **Material You设计** - 现代化的UI设计，符合Google最新设计语言
- 📱 **多设备支持** - 支持连接和管理多个Android设备
- 📂 **直观的文件导航** - 带路径导航栏的文件浏览器，支持点击路径跳转
- 📝 **文件编辑功能** - 内置文本编辑器，支持编辑设备上的文本文件
- 🔒 **权限检查** - 智能检测并提示文件操作权限问题
- 🔄 **实时反馈** - 所有操作都有实时状态反馈
- 📥 **文件传输** - 支持从设备下载文件和上传文件到设备

### 🔧 技术栈

- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 现代化UI工具包
- **ADB** - Android Debug Bridge工具
- **Coroutines** - 处理异步操作
- **Flow** - 响应式数据流

### 🚀 安装

#### 从源码构建

1. 克隆仓库
   ```bash
   git clone https://github.com/wkbin/AdbFileManager.git
   cd AdbFileManager
   ```

2. 使用Gradle构建
   ```bash
   ./gradlew build
   ```

3. 运行应用
   ```bash
   ./gradlew run
   ```

### 💡 使用说明

1. **连接设备**：
   - 确保您已启用设备上的USB调试模式
   - 将设备连接到计算机
   - 从下拉菜单选择您的设备

2. **浏览文件**：
   - 点击文件夹进入
   - 使用路径导航栏快速跳转到上级目录
   - 点击刷新按钮更新文件列表

3. **文件操作**：
   - 点击文件旁的菜单按钮查看可用操作
   - 编辑：修改文本文件内容
   - 下载：将文件保存到本地计算机
   - 删除：从设备移除文件

4. **创建文件夹**：
   - 点击工具栏中的"新建文件夹"按钮
   - 输入文件夹名称并确认

### 📋 待实现功能

- [ ] 文件搜索功能
- [ ] 文件权限修改
- [ ] 深色/浅色主题切换
- [ ] 拖放上传文件
- [ ] 文件预览功能

### 🤝 贡献

欢迎贡献代码、报告问题或提出功能建议！请查看[贡献指南](CONTRIBUTING.md)了解详情。

1. Fork这个仓库
2. 创建您的功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交Pull Request

### 📄 许可证

本项目基于MIT许可证发布 - 详情请查看[LICENSE](LICENSE)文件。
