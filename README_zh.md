# ADB文件管理器

*阅读：[English](README.md) | [中文](README_zh.md)*

一个基于Material You设计的Android设备文件管理器，通过ADB连接实现远程文件操作。

![Kotlin](https://img.shields.io/badge/kotlin-2.1.20-blue.svg)
![Compose](https://img.shields.io/badge/compose-1.8.0-green.svg)
![Version](https://img.shields.io/badge/version-v2.6.0-orange.svg)

## 📖 简介

ADB File Manager是一款桌面应用程序，允许你通过ADB连接远程浏览和管理Android设备文件系统。采用Jetpack Compose构建的现代Material You界面，提供流畅的用户体验和优雅的动画效果。

## 📸 预览

<div align="center">
  <img src="docs/images/1.png" alt="ADB文件管理器主页预览" width="800">
  <img src="docs/images/2.png" alt="ADB文件管理器编辑内容预览" width="800">
  <img src="docs/images/3.png" alt="ADB文件管理器深色模式搜索预览" width="800">
</div>

## ✨ 特点

- 🎨 **Material You设计** - 现代化的UI设计，符合Google最新设计语言
- 📱 **多设备支持** - 支持连接和管理多个Android设备
- 📂 **直观的文件导航** - 带路径导航栏的文件浏览器，支持点击路径跳转
- 🔍 **目录跳转功能** - 通过面包屑导航轻松遍历目录，支持快速跳转到任意父级目录
- 📝 **文件编辑功能** - 内置文本编辑器，支持编辑设备上的文本文件
- 🔒 **权限检查** - 智能检测并提示文件操作权限问题
- 🔄 **实时反馈** - 所有操作都有实时状态反馈
- 📥 **文件传输** - 支持从设备下载文件和上传文件到设备
- 🌓 **深色/浅色主题** - 支持在深色和浅色主题间切换，适应不同环境使用
- 📄 **文件创建** - 支持在设备上直接创建新的文本文件并添加自定义内容
- 🔍 **文件搜索** - 在当前目录中搜索文件和文件夹
- 🔄 **高级排序** - 按名称、类型（文件夹优先或文件优先）、日期和大小进行升序或降序排序
- 📁 **文件夹导入** - 一键导入本地文件夹到设备
- 🔖 **书签功能** - 将常用目录保存为书签，实现快速导航
- 📊 **平铺视图** - 支持列表和网格两种查看模式，更直观地显示文件
- 🖼️ **拖放上传与下载** - 通过拖放文件到应用程序中轻松上传文件，同时支持拖放下载文件

## 🔧 技术栈

- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 现代化UI工具包
- **ADB** - Android Debug Bridge工具
- **Coroutines** - 处理异步操作
- **Flow** - 响应式数据流

## 🚀 安装

### 从源码构建

1. 克隆仓库
   ```bash
   git clone https://github.com/AdbFileManager.git
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

## 💡 使用说明

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

5. **创建文件**：
   - 点击工具栏中的"新建文件"按钮
   - 输入文件名和可选的文件内容
   - 确认以在当前目录创建文件

6. **导入文件和文件夹**：
   - 点击"导入文件"按钮上传单个文件
   - 点击"导入文件夹"按钮上传整个文件夹
   - 从本地系统选择文件或文件夹
   - 内容将自动上传到当前目录

7. **切换主题**：
   - 点击工具栏中的主题图标
   - 在系统默认、浅色主题和深色主题之间切换

8. **搜索文件**：
   - 点击工具栏中的搜索图标
   - 输入搜索关键词
   - 结果将实时显示

9. **排序文件**：
   - 点击工具栏中的排序图标
   - 选择不同的排序方式：
     - 类型（文件夹优先或文件优先）
     - 名称（A-Z或Z-A）
     - 日期（最早或最新）
     - 大小（最小或最大）

## 📋 待实现功能

- [ ] 文件权限修改
- [ ] 文件预览功能
- [ ] 多文件选择批量操作

## 🤝 贡献

欢迎贡献代码、报告问题或提出功能建议！请查看[贡献指南](CONTRIBUTING.md)了解详情。

1. Fork这个仓库
2. 创建您的功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交Pull Request

## 📈 Star 趋势

[![Star History Chart](https://starchart.cc/wkbin/AdbFileManager.svg)](https://starchart.cc/wkbin/AdbFileManager)

## 📄 许可证

本项目基于MIT许可证发布 - 详情请查看[LICENSE](LICENSE)文件。 