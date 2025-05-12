package viewmodel

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import model.FileItem
import model.FileUtils
import runtime.adb.AdbDevicePoller
import java.io.File
import org.mozilla.universalchardet.UniversalDetector
import java.nio.charset.Charset
import utils.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Serializable
data class Bookmark(
    val name: String,
    val path: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val body: String,
    val html_url: String
)

/**
 * ViewModel for file manager operations
 */
class FileManagerViewModel(
    private val adbDevicePoller: AdbDevicePoller,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        const val GITHUB_URL = "https://github.com/wkbin/AdbFileManager"
        const val VERSION = "2.5.0"
    }

    // Current directory path components
    private val _directoryPath = mutableStateListOf<String>()
    val directoryPath: SnapshotStateList<String> = _directoryPath

    // Current list of files
    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Success state
    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    // Current file being edited
    val currentFileName = mutableStateOf("")
    val currentFileContent = mutableStateOf("")
    val currentFileEncoding = mutableStateOf("UTF-8")

    // 搜索结果
    private val _searchResults = MutableStateFlow<List<FileItem>>(emptyList())
    val searchResults: StateFlow<List<FileItem>> = _searchResults.asStateFlow()

    // 搜索状态
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // 排序方式
    private val _sortType = MutableStateFlow(SortType.TYPE_ASC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    // 排序触发状态
    private val _sortTrigger = MutableStateFlow(0)
    val sortTrigger: StateFlow<Int> = _sortTrigger.asStateFlow()

    // 视图模式
    private val _viewMode = mutableStateOf(ViewMode.LIST)
    val viewMode: State<ViewMode> = _viewMode

    // 书签相关
    private val _bookmarks = mutableStateListOf<Bookmark>()
    val bookmarks: List<Bookmark> = _bookmarks

    private val bookmarksFile: File by lazy {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".adbfilemanager")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        File(appDir, "bookmarks.json")
    }

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    init {
        loadBookmarks()
        checkForUpdates()
    }

    /**
     * 从JSON文件加载书签
     */
    private fun loadBookmarks() {
        try {
            if (bookmarksFile.exists()) {
                val format = Json { ignoreUnknownKeys = true; isLenient = true }
                val json = bookmarksFile.readText()
                val loadedBookmarks = format.decodeFromString<List<Bookmark>>(json)
                _bookmarks.clear()
                _bookmarks.addAll(loadedBookmarks)
            }
        } catch (e: Exception) {
            println("加载书签失败: ${e.message}")
        }
    }

    /**
     * 将书签保存到JSON文件
     */
    private fun saveBookmarks() {
        try {
            val format = Json { prettyPrint = true; encodeDefaults = true }
            val json = format.encodeToString(_bookmarks.toList())
            bookmarksFile.writeText(json)
        } catch (e: Exception) {
            println("保存书签失败: ${e.message}")
        }
    }

    /**
     * 设置排序方式
     */
    fun setSortType(type: SortType) {
        _sortType.value = type
        sortFiles()
    }

    /**
     * 对文件列表进行排序
     */
    private fun sortFiles() {
        val currentFiles = _files.value.toMutableList()
        when (_sortType.value) {
            SortType.NAME_ASC -> currentFiles.sortBy { it.fileName.lowercase() }
            SortType.TYPE_ASC -> currentFiles.sortWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.fileName.lowercase() })
            SortType.TYPE_DESC -> currentFiles.sortWith(compareBy<FileItem> { it.isDir }.thenByDescending { it.fileName.lowercase() })
            SortType.DATE_ASC -> currentFiles.sortBy { it.date }
            SortType.DATE_DESC -> currentFiles.sortByDescending { it.date }
            SortType.SIZE_ASC -> currentFiles.sortBy { it.size }
            SortType.SIZE_DESC -> currentFiles.sortByDescending { it.size }
        }
        _files.value = currentFiles
        // 触发排序事件，通知UI重置滚动位置
        _sortTrigger.value = _sortTrigger.value + 1
    }

    /**
     * Load files from the current directory
     */
    fun loadFiles() {
        _isLoading.value = true
        _error.value = null

        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                adbDevicePoller.exec("shell ls -l -p /${dirPath} | sort") { result ->
                    // 检查权限错误
                    if (result.any { it.contains("Permission denied") }) {
                        setError("权限不足：无法访问该目录")
                        _files.value = emptyList()
                    } else if (
                        result.firstOrNull()?.startsWith("ls") == true ||
                        result.lastOrNull()?.contains("Permission") == true ||
                        result.lastOrNull()?.contains("directory") == true
                    ) {
                        _files.value = emptyList()
                    } else {
                        _files.value = FileUtils.parseLsOutput(result)
                        sortFiles() // 应用当前排序方式
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                setError("加载文件列表失败: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * Navigate to a directory
     * 支持普通目录和软链接目录（绝对路径和相对路径）
     */
    fun navigateTo(directoryName: String) {
        // 保存当前路径列表的副本，以便在出错时恢复
        val previousPath = _directoryPath.toList()

        // 检查是否为软链接路径
        val isSymlink = directoryName.contains("/") || directoryName.contains("\\")

        // 处理目录路径
        if (isSymlink) {
            // 软链接可能是指向绝对路径或相对路径
            if (directoryName.startsWith("/")) {
                // 绝对路径 - 重置目录路径并导航到链接目标
                _directoryPath.clear()
                // 移除开头的斜杠，分割路径组件，并过滤掉空字符串
                val pathComponents = directoryName.substring(1).split("/").filter { it.isNotEmpty() }
                _directoryPath.addAll(pathComponents)
            } else {
                // 相对路径 - 相对于当前目录
                val pathComponents = directoryName.split("/").filter { it.isNotEmpty() }

                // 处理特殊情况如 "../../path"
                for (component in pathComponents) {
                    when (component) {
                        "." -> continue // 当前目录，不操作
                        ".." -> {
                            // 上一级目录，如果有目录可以移除则移除
                            if (_directoryPath.isNotEmpty()) {
                                _directoryPath.removeLast()
                            }
                        }

                        else -> _directoryPath.add(component) // 正常目录名，添加
                    }
                }
            }
        } else {
            // 普通目录，直接添加
            _directoryPath.add(directoryName)
        }

        // 验证目录是否可访问
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                // 先检查目录是否可访问
                adbDevicePoller.exec("shell cd /${dirPath} && echo SUCCESS || echo FAILURE") { result ->
                    if (result.any { it.contains("Permission denied") || it.contains("FAILURE") || it.contains("No such file") }) {
                        // 恢复之前的路径
                        _directoryPath.clear()
                        _directoryPath.addAll(previousPath)
                        _error.value = "无法访问目录: 权限不足或目录不存在"
                    }
                    // 无论成功与否，都加载文件列表
                    loadFiles()
                }
            } catch (e: Exception) {
                // 出现异常时，也恢复路径
                _directoryPath.clear()
                _directoryPath.addAll(previousPath)
                _error.value = "访问目录出错: ${e.message}"
                loadFiles()
            }
        }
    }

    /**
     * Navigate up one directory
     */
    fun navigateUp() {
        if (_directoryPath.isNotEmpty()) {
            _directoryPath.removeLast()
            loadFiles()
        }
    }

    /**
     * Delete a file or directory
     */
    fun deleteFile(fileName: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                currentFileName.value = fileName
                adbDevicePoller.exec("shell rm -rf /${dirPath}/${fileName}") { result ->
                    if (result.any { it.contains("Permission denied") }) {
                        setError("权限不足，无法删除文件")
                    } else if (result.any { it.contains("No such file") }) {
                        setError("文件不存在")
                    } else if (result.any { it.contains("Directory not empty") }) {
                        setError("目录不为空，无法删除")
                    } else if (result.any { it.contains("Error:") }) {
                        setError(result.first { it.contains("Error:") })
                    } else {
                        setSuccess("文件删除成功")
                        onSuccess()
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                setError("删除文件时发生错误: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a new file
     */
    fun createFile(fileName: String, content: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                val escapedContent = content.replace("\"", "\\\"")
                adbDevicePoller.exec("shell echo \"$escapedContent\" > /${dirPath}/${fileName}") { result ->
                    if (result.any { it.contains("Permission denied") }) {
                        setError("权限不足，无法创建文件")
                    } else if (result.any { it.contains("Read-only file system") }) {
                        setError("文件系统为只读，无法创建文件")
                    } else if (result.any { it.contains("No space left") }) {
                        setError("设备存储空间不足")
                    } else if (result.any { it.contains("Error:") }) {
                        setError(result.first { it.contains("Error:") })
                    } else {
                        setSuccess("文件创建成功")
                        onSuccess()
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                setError("创建文件时发生错误: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a new directory
     */
    fun createDirectory(dirName: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                adbDevicePoller.exec("shell mkdir /${dirPath}/${dirName}") { result ->
                    if (result.any { it.contains("Permission denied") }) {
                        setError("权限不足，无法创建目录")
                    } else if (result.any { it.contains("Read-only file system") }) {
                        setError("文件系统为只读，无法创建目录")
                    } else if (result.any { it.contains("No space left") }) {
                        setError("设备存储空间不足")
                    } else if (result.any { it.contains("File exists") }) {
                        setError("目录已存在")
                    } else if (result.any { it.contains("Error:") }) {
                        setError(result.first { it.contains("Error:") })
                    } else {
                        setSuccess("目录创建成功")
                        onSuccess()
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                setError("创建目录时发生错误: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * Load file content for editing
     */
    fun loadFileContent(
        fileName: String,
        useDetectedEncoding: Boolean = true,
        onSuccess: () -> Unit
    ) {
        _isLoading.value = true
        _error.value = null
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                val tempFile = File.createTempFile("pull_", ".tmp")
                adbDevicePoller.exec("pull /${dirPath}/${fileName} \"${tempFile.absolutePath}\"") { result ->
                    if (result.any { it.contains("error") || it.contains("failed") }) {
                        setError("拉取文件失败: ${result.joinToString("\n")}")
                        tempFile.delete()
                    } else {
                        // 只在首次加载时检测编码
                        if (useDetectedEncoding) {
                            val encoding = detectEncoding(tempFile)
                            currentFileEncoding.value = encoding
                        }
                        val content = tempFile.readText(Charset.forName(currentFileEncoding.value))
                        currentFileContent.value = content
                        currentFileName.value = fileName
                        tempFile.delete()
                        onSuccess()
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                setError("读取文件时发生错误: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * Save edited file content
     */
    fun saveFileContent(content: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null
        coroutineScope.launch {
            try {
                val fileName = currentFileName.value ?: return@launch
                val dirPath = _directoryPath.joinToString("/")

                // 将内容写入临时文件，使用检测到的编码
                val tempFile = File.createTempFile("temp_", ".txt")
                tempFile.writeText(content.replace("\r\n", "\n"), Charset.forName(currentFileEncoding.value))

                // 使用adb push命令将临时文件推送到设备
                adbDevicePoller.exec("push \"${tempFile.absolutePath}\" \"/${dirPath}/${fileName}\"") { result ->
                    // 删除临时文件
                    tempFile.delete()

                    if (result.any { it.contains("error") || it.contains("failed") }) {
                        setError("保存文件失败: ${result.joinToString("\n")}")
                    } else {
                        setSuccess("文件保存成功")
                        onSuccess()
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                setError("保存文件时发生错误: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * Pull file to local device
     */
    fun pullFile(fileName: String, destinationPath: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                adbDevicePoller.exec("pull /${dirPath}/${fileName} $destinationPath") { result ->
                    if (result.any { it.contains("error") || it.contains("failed") }) {
                        setError("导出文件失败: ${result.joinToString("\n")}")
                    } else {
                        setSuccess("文件导出成功")
                    }
                    _isLoading.value = false
                    onSuccess()
                }
            } catch (e: Exception) {
                setError("导出文件失败: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * Push file to device
     */
    fun pushFile(localFilePath: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                adbDevicePoller.exec("push $localFilePath /$dirPath") { result ->
                    if (result.any { it.contains("error") || it.contains("failed") }) {
                        setError("导入文件失败: ${result.joinToString("\n")}")
                    } else {
                        setSuccess("文件导入成功")
                    }
                    _isLoading.value = false
                    onSuccess()
                }
            } catch (e: Exception) {
                setError("导入文件失败: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * Navigate to a specific path index
     * @param index the index to navigate to (-1 for root)
     */
    fun navigateToPathIndex(index: Int) {
        if (index < -1 || index >= _directoryPath.size) return

        // 保存当前路径的副本，以便在出错时恢复
        val previousPath = _directoryPath.toList()

        // 如果是根目录
        if (index == -1) {
            _directoryPath.clear()
            loadFiles()
            return
        } else {
            // 移除所有大于index的路径组件
            while (_directoryPath.size > index + 1) {
                _directoryPath.removeLast()
            }
        }

        // 检查目录权限
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                // 先检查目录是否可访问
                adbDevicePoller.exec("shell cd /${dirPath} && echo SUCCESS || echo FAILURE") { result ->
                    if (result.any { it.contains("Permission denied") || it.contains("FAILURE") }) {
                        // 恢复之前的路径
                        _directoryPath.clear()
                        _directoryPath.addAll(previousPath)
                        _error.value = "权限不足：无法访问该目录"
                    }
                    // 无论成功与否，都加载文件列表
                    loadFiles()
                }
            } catch (e: Exception) {
                // 出现异常时，也恢复路径
                _directoryPath.clear()
                _directoryPath.addAll(previousPath)
                _error.value = "Error accessing directory: ${e.message}"
                loadFiles()
            }
        }
    }

    /**
     * Import a file from local system to current directory
     */
    fun importFile(file: java.io.File, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null

        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                val localFilePath = file.absolutePath

                adbDevicePoller.exec("push \"$localFilePath\" \"/${dirPath}/\"") { result ->
                    if (result.any { it.contains("error") || it.contains("failed") }) {
                        setError("导入文件失败: ${result.joinToString("\n")}")
                    } else {
                        setSuccess("文件导入成功")
                        onSuccess()
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                setError("导入文件失败: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * Import a folder from local system to current directory
     */
    fun importFolder(folderPath: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null

        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                val folderName = File(folderPath).name

                // 先在目标目录创建同名文件夹
                adbDevicePoller.exec("push \"$folderPath\" \"/${dirPath}/${folderName}/\"") { pushResult ->
                    if (pushResult.any { it.contains("error") || it.contains("failed") }) {
                        setError("导入文件夹失败: ${pushResult.joinToString("\n")}")
                    } else {
                        setSuccess("文件夹导入成功")
                        onSuccess()
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                setError("导入文件夹失败: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * 刷新当前目录内容
     */
    fun reload() {
        loadFiles()
    }

    /**
     * 检查是否可以向上导航
     */
    fun canNavigateUp(): Boolean {
        return _directoryPath.isNotEmpty()
    }

    // 设置错误消息并自动清除
    fun setError(message: String) {
        _error.value = message
        coroutineScope.launch {
            kotlinx.coroutines.delay(3000) // 3秒后自动清除错误消息
            _error.value = null
        }
    }

    // 设置成功消息并自动清除
    private fun setSuccess(message: String) {
        _success.value = message
        coroutineScope.launch {
            kotlinx.coroutines.delay(3000) // 3秒后自动清除成功消息
            _success.value = null
        }
    }

    // 清除错误消息
    fun clearError() {
        _error.value = null
    }

    // 清除成功消息
    fun clearSuccess() {
        _success.value = null
    }

    /**
     * 搜索文件
     */
    fun searchFiles(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        _isSearching.value = true

        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                adbDevicePoller.exec("shell ls -l -p /${dirPath} | grep -i \"${query}\" | sort") { result ->
                    // 检查权限错误
                    if (result.any { it.contains("Permission denied") }) {
                        setError("权限不足：无法搜索该目录")
                        _searchResults.value = emptyList()
                    } else if (
                        result.firstOrNull()?.startsWith("ls") == true ||
                        result.lastOrNull()?.contains("Permission") == true ||
                        result.lastOrNull()?.contains("directory") == true
                    ) {
                        _searchResults.value = emptyList()
                    } else {
                        _searchResults.value = FileUtils.parseLsOutput(result)
                    }
                    _isSearching.value = false
                }
            } catch (e: Exception) {
                setError("搜索文件失败: ${e.message}")
                _isSearching.value = false
            }
        }
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    /**
     * 添加书签并持久化保存
     * @param name 书签名称，如果为空或空白字符串，则使用当前路径最后一个组件作为名称
     */
    fun addBookmark(name: String? = null) {
        val currentPath = directoryPath.joinToString("/")
        // 如果未提供名称或名称为空，使用当前路径最后一个组件作为书签名
        val bookmarkName = if (name.isNullOrBlank()) {
            // 如果路径为空，使用"根目录"作为名称
            if (directoryPath.isEmpty()) {
                "根目录"
            } else {
                // 否则使用路径的最后一个组件
                directoryPath.last()
            }
        } else {
            name
        }

        val bookmark = Bookmark(bookmarkName, currentPath)
        _bookmarks.add(bookmark)
        saveBookmarks()
    }

    /**
     * 删除书签并持久化保存
     */
    fun removeBookmark(bookmark: Bookmark) {
        _bookmarks.remove(bookmark)
        saveBookmarks()
    }

    /**
     * 导航到书签的路径
     */
    fun navigateToBookmark(bookmark: Bookmark) {
        directoryPath.clear()
        directoryPath.addAll(bookmark.path.split("/").filter { it.isNotEmpty() })
        loadFiles()
    }

    // 自动检测文件编码
    private fun detectEncoding(file: File): String {
        val detector = UniversalDetector(null)
        file.inputStream().use { input ->
            val buffer = ByteArray(4096)
            var nread: Int
            while (input.read(buffer).also { nread = it } > 0) {
                detector.handleData(buffer, 0, nread)
            }
        }
        detector.dataEnd()
        return detector.detectedCharset ?: "UTF-8"
    }

    private fun checkForUpdates() {
        coroutineScope.launch {
            try {
                val latestRelease = withContext(Dispatchers.IO) {
                    val response = URL("https://api.github.com/repos/$GITHUB_URL/releases/latest").readText()
                    Json.decodeFromString<GitHubRelease>(response)
                }

                // 比较版本号
                val currentVersion = VERSION.replace("v", "")
                val latestVersion = latestRelease.tag_name.replace("v", "")
                
                if (isNewerVersion(latestVersion, currentVersion)) {
                    _updateInfo.value = UpdateInfo(
                        version = latestRelease.tag_name,
                        releaseNotes = latestRelease.body,
                        downloadUrl = latestRelease.html_url
                    )
                    _showUpdateDialog.value = true
                }
            } catch (e: Exception) {
                // 处理检查更新失败的情况
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toInt() }
        val currentParts = current.split(".").map { it.toInt() }
        
        for (i in 0 until minOf(latestParts.size, currentParts.size)) {
            if (latestParts[i] > currentParts[i]) return true
            if (latestParts[i] < currentParts[i]) return false
        }
        
        return latestParts.size > currentParts.size
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }
}

/**
 * 排序类型枚举
 */
enum class SortType(val displayName: String) {
    TYPE_ASC("类型 (文件夹优先)"),
    TYPE_DESC("类型 (文件优先)"),
    NAME_ASC("名称 (A-Z)"),
    DATE_ASC("日期 (最早)"),
    DATE_DESC("日期 (最新)"),
    SIZE_ASC("大小 (最小)"),
    SIZE_DESC("大小 (最大)")
}

/**
 * 文件视图模式
 */
enum class ViewMode {
    LIST, GRID
} 