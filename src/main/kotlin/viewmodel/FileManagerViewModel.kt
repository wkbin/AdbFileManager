package viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.FileItem
import model.FileUtils
import runtime.adb.AdbDevicePoller

/**
 * ViewModel for file manager operations
 */
class FileManagerViewModel(
    private val adbDevicePoller: AdbDevicePoller,
    private val coroutineScope: CoroutineScope
) {
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
    fun loadFileContent(fileName: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                adbDevicePoller.exec("shell cat /${dirPath}/${fileName}") { result ->
                    if (result.any { it.contains("unknown command") }) {
                        setError("ADB命令执行失败：未知命令")
                    } else if (result.any { it.contains("Permission denied") }) {
                        setError("权限不足，无法读取文件")
                    } else if (result.any { it.contains("No such file") }) {
                        setError("文件不存在")
                    } else if (result.any { it.contains("Is a directory") }) {
                        setError("无法读取目录内容")
                    } else if (result.any { it.contains("Error:") }) {
                        setError(result.first { it.contains("Error:") })
                    } else {
                        currentFileContent.value = result.joinToString("\n")
                        currentFileName.value = fileName
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
                
                // 将内容写入临时文件
                val tempFile = java.io.File.createTempFile("temp_", ".txt")
                tempFile.writeText(content)
                
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
    private fun setError(message: String) {
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
} 