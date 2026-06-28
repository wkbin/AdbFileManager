package model

class ZhStrings : Strings {
    override val appTitle = "ADB 文件管理器"
    override fun appVersion(version: String) = "版本 $version"
    override val appDescription = "一个简单易用的 ADB 文件管理工具，支持文件传输、编辑和管理。"
    override val appChecking = "检查中..."
    override val appCheckUpdate = "检查更新"
    override val appViewOnGithub = "在 GitHub 上查看"
    override val appClose = "关闭"
    override val appLater = "稍后再说"
    override val appUpdateNow = "立即更新"
    override val appNewVersion = "发现新版本"
    override fun appNewVersionDetail(version: String) = "新版本 $version 已发布"
    override val appUpdateContent = "更新内容："
    override val appDontPromptAgain = "不再提示版本更新"

    override val navRoot = "根目录"
    override val navBack = "返回"
    override val navGoBack = "返回顶部"

    override val fileDeleteConfirm = "确认删除"
    override fun fileDeleteMessage(fileName: String) = "确定要删除 \"$fileName\" 吗？此操作不可撤销。"
    override val fileDelete = "删除"
    override val fileCancel = "取消"
    override val fileSave = "保存"
    override val fileRename = "重命名"
    override val fileCopy = "复制"
    override val fileMove = "移动"
    override val fileCopyTo = "复制到..."
    override val fileMoveTo = "移动到..."
    override val copySuccess = "复制成功"
    override fun copyFailed(error: String) = "复制失败: $error"
    override val moveSuccess = "移动成功"
    override fun moveFailed(error: String) = "移动失败: $error"
    override val destinationPath = "目标路径"
    override val fileImportSuccess = "导入文件成功"
    override fun fileImportFailed(error: String) = "导入文件失败: $error"
    override fun fileExportFailed(error: String) = "导出文件失败: $error"
    override val fileDeleteSuccess = "删除文件成功"
    override fun fileDeleteFailed(error: String) = "删除文件失败: $error"
    override val fileSaveSuccess = "保存文件成功"
    override fun fileSaveFailed(error: String) = "保存文件失败: $error"
    override val fileDownload = "下载"
    override val fileInstallApk = "安装 APK"
    override val apkInstallSuccess = "APK 安装成功"
    override fun apkInstallFailed(error: String) = "APK 安装失败: $error"
    override val fileEdit = "编辑"
    override val fileNewFolder = "创建新文件夹"
    override val fileNewFile = "创建新文件"
    override val fileFolderName = "文件夹名称"
    override val fileFolderNameInput = "输入新文件夹名称"
    override val fileFolderNameErrorInvalid = "文件夹名称不能包含特殊字符如: / 或 \\"
    override val fileName = "文件名"
    override val fileNameInput = "输入文件名 (例如: note.txt)"
    override val fileContent = "文件内容"
    override val fileContentInput = "输入文件内容 (可选)"
    override val fileCreate = "创建"
    override val fileNameEmptyError = "文件名不能为空"
    override val fileNameInvalidError = "文件名不能包含特殊字符"
    override val fileLink = "链接"
    override val fileLinkDirectory = "链接目录"
    override val fileDirectory = "目录"
    override val fileModified = "已修改"
    override fun fileCharactersLines(chars: Int, lines: Int) = "$chars 字符, $lines 行"

    override val editorTitle = "编辑"
    override fun editorTitleWithName(name: String) = "编辑 - $name"
    override val editorFileEditor = "文件编辑器"
    override fun editorEncoding(encoding: String) = "编码: $encoding"
    override val editorEncodingLabel = "编码: "
    override val editorClose = "关闭"
    override val editorFileContent = "编辑文件内容"
    override val editorModified = "已修改"
    override val editorCharactersLines = ""

    override val sortTitle = "排序"
    override val sortTypeFolderFirst = "类型 (文件夹优先)"
    override val sortTypeFileFirst = "类型 (文件优先)"
    override val sortNameAZ = "名称 (A-Z)"
    override val sortDateOldest = "日期 (最早)"
    override val sortDateNewest = "日期 (最新)"
    override val sortSizeSmallest = "大小 (最小)"
    override val sortSizeLargest = "大小 (最大)"

    override val pathEdit = "编辑路径"
    override val pathInput = "输入路径"
    override val pathInputPlaceholder = "输入目标路径"
    override fun pathInvalid(path: String) = "无效路径: $path"
    override val pathAlreadyRoot = "已经是根目录了"
    override val pathInvalidIndex = "无效的索引"
    override val pathCollapse = "折叠"

    override val searchTitle = "搜索文件"
    override val searchPlaceholder = "搜索..."

    override val errorTitle = "出错了"
    override val errorEmptyDirectory = "当前目录为空"
    override val errorNoFiles = "没有找到任何文件或文件夹"
    override val errorLoading = "正在加载..."

    override val permissionDenied = "权限不足"
    override val permissionInvalid = "无效路径"

    override fun terminalCommandFailed(exitCode: Int, msg: String) = "命令执行失败 (exitCode=$exitCode): $msg"

    override fun adbPermissionSet(path: String) = "已自动设置 ADB 可执行权限: $path"
    override fun adbPermissionError(msg: String) = "设置 ADB 可执行权限时出错: $msg"

    // About dialog strings
    override val aboutTitle = "关于"
    override val aboutDescription = "一个简单易用的 ADB 文件管理工具，支持文件传输、编辑和管理。"
    override val aboutViewOnGithub = "在 GitHub 上查看"
    override val aboutCheckUpdate = "检查更新"
    override val aboutChecking = "检查中..."
    override val aboutClose = "关闭"

    // Theme strings
    override val themeFollowSystem = "跟随系统"
    override val themeLight = "亮色模式"
    override val themeDark = "暗色模式"
    override val themeToggle = "切换主题"
    override val languageToggle = "切换语言"
    override val deviceToggle = "切换设备"

    // Language strings
    override val languageChinese = "中文"
    override val languageEnglish = "English"

    // Device strings
    override val deviceNotConnected = "未连接"
    override val deviceSelectMethod = "请选择连接方式"

    // Window strings
    override val windowMinimize = "最小化"
    override val windowClose = "关闭"

    // Device connection strings
    override val deviceConnectUSB = "USB连接"
    override val deviceConnectWireless = "无线连接"
    override val deviceUSBGuide = "USB连接向导"
    override val deviceWirelessGuide = "无线连接"
    override val deviceRefresh = "刷新"
    override val deviceConnect = "连接"
    override fun deviceUSBGuideStep1() = "在Android设备上启用开发者选项："
    override fun deviceUSBGuideStep2() = "启用USB调试："
    override fun deviceUSBGuideStep3() = "连接设备："
    override fun deviceUSBGuideStep4() = "点击刷新按钮检查连接"
    override fun deviceWirelessGuideStep1() = "确保设备和电脑在同一网络下"
    override fun deviceWirelessGuideStep2() = "在设备上启用无线调试："
    override fun deviceWirelessGuideStep3() = "连接方式："
    override fun deviceWirelessGuideStep4() = "方式：手动输入配对信息（已配对设备可不输入配对端口和配对码）"
    override val deviceWirelessGuideIP = "IP地址"
    override val deviceWirelessGuidePort = "端口（不填默认：5555）"
    override val deviceWirelessGuidePairPort = "配对端口（选填）"
    override val deviceWirelessGuidePairCode = "配对码（选填）"
    
    // Permission strings
    override val changePermissions = "修改权限"
    override val permissionChangeSuccess = "权限修改成功"
    override fun permissionChangeFailed(error: String) = "权限修改失败: $error"
    override fun permissionCurrent(permissions: String) = "当前权限: $permissions"
    override val permissionUseOctal = "使用八进制"
    override val permissionOctalLabel = "八进制权限 (如: 755)"
    override val permissionOwner = "所有者 (User)"
    override val permissionGroup = "组 (Group)"
    override val permissionOther = "其他 (Other)"
    override val permissionRead = "读"
    override val permissionWrite = "写"
    override val permissionExecute = "执行"
    
    // Bookmark strings
    override val addBookmark = "添加书签"
    override val removeBookmark = "取消书签"
    override val bookmark = "书签"
    override fun bookmarkPath(currentPath: String): String {
        return "路径: $currentPath"
    }
    override val bookmarkNameLabel = "书签名称"
    override val bookmarkConfirm = "添加"
    override val bookmarkCancel = "取消"
    override val bookmarkEmpty = "暂无书签"
    override val bookmarkAddCurrentPath = "添加当前路径"
    override val bookmarkAddedSuccess = "书签添加成功"
    override fun bookmarkAddedFailed(error: String) = "书签添加失败: $error"
    override val bookmarkRemovedSuccess = "书签删除成功"
    override fun bookmarkRemovedFailed(error: String) = "书签删除失败: $error"
    override val folderCreatedSuccess = "文件夹创建成功"
    override fun folderCreatedFailed(error: String) = "文件夹创建失败: $error"
    override val fileCreatedSuccess = "文件创建成功"
    override fun fileCreatedFailed(error: String) = "文件创建失败: $error"
    override val renameSuccess = "重命名成功"
    override fun renameFailed(error: String) = error
    
    // Transfer strings
    override val transferFile = "传输文件"
    override val transferPreparing = "准备中..."
    override val transferComplete = "传输完成"
    override fun transferProgress(bytesTransferred: Long, totalBytes: Long): String {
        val transferred = formatFileSize(bytesTransferred)
        val total = formatFileSize(totalBytes)
        return "$transferred / $total"
    }
    override fun transferInProgress(percent: Int) = "传输中... (约 $percent%)"

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
            else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
        }
    }
}
