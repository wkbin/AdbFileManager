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
                        _error.value = "权限不足：无法访问该目录"
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
                _error.value = "Error loading files: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Navigate to a directory
     */
    fun navigateTo(directoryName: String) {
        // 保存当前路径列表的副本，以便在出错时恢复
        val previousPath = _directoryPath.toList()
        
        // 添加新目录并尝试加载
        _directoryPath.add(directoryName)
        
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                // 先检查目录是否可访问
                adbDevicePoller.exec("shell cd /${dirPath} && echo SUCCESS || echo FAILURE") { result ->
                    if (result.any { it.contains("Permission denied") || it.contains("FAILURE") }) {
                        // 恢复之前的路径
                        _directoryPath.clear()
                        _directoryPath.addAll(previousPath)
                        _error.value = "权限不足：无法访问 ${directoryName} 目录"
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
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                currentFileName.value = fileName
                adbDevicePoller.exec("shell rm -rf /${dirPath}/${fileName}") {
                    _isLoading.value = false
                    onSuccess()
                }
            } catch (e: Exception) {
                _error.value = "Error deleting file: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Create a directory
     */
    fun createDirectory(dirName: String, onSuccess: () -> Unit) {
        if (dirName.isEmpty()) return
        
        _isLoading.value = true
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                adbDevicePoller.exec("shell mkdir /$dirPath/$dirName") {
                    _isLoading.value = false
                    onSuccess()
                }
            } catch (e: Exception) {
                _error.value = "Error creating directory: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Create a new file with content
     */
    fun createFile(fileName: String, content: String, onSuccess: () -> Unit) {
        if (fileName.isEmpty()) return
        
        _isLoading.value = true
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                val escapedContent = content.replace("\"", "\\\"")
                val filePath = "/$dirPath/$fileName"
                
                // 先创建空文件，然后写入内容
                adbDevicePoller.exec("""shell "echo '$escapedContent' > $filePath"""") {
                    _isLoading.value = false
                    onSuccess()
                }
            } catch (e: Exception) {
                _error.value = "创建文件失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load file content for editing
     */
    fun loadFileContent(fileName: String, onContentLoaded: (String) -> Unit) {
        _isLoading.value = true
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                currentFileName.value = fileName
                adbDevicePoller.exec("shell cat /${dirPath}/${fileName}") { result ->
                    val content = result.joinToString("\n")
                    currentFileContent.value = if (fileName.endsWith(".json")) {
                        FileUtils.formatJson(content)
                    } else {
                        content
                    }
                    _isLoading.value = false
                    onContentLoaded(currentFileContent.value)
                }
            } catch (e: Exception) {
                _error.value = "Error loading file content: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Save edited file content
     */
    fun saveFileContent(content: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        coroutineScope.launch {
            try {
                val dirPath = _directoryPath.joinToString("/")
                val escapedContent = content.replace("\"", "\\\"")
                adbDevicePoller.exec(
                    """shell "cat > /${dirPath}/${currentFileName.value}" <<< '$escapedContent'"""
                ) {
                    _isLoading.value = false
                    onSuccess()
                }
            } catch (e: Exception) {
                _error.value = "Error saving file: ${e.message}"
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
                adbDevicePoller.exec("pull /${dirPath}/${fileName} $destinationPath") {
                    _isLoading.value = false
                    onSuccess()
                }
            } catch (e: Exception) {
                _error.value = "Error pulling file: ${e.message}"
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
                adbDevicePoller.exec("push $localFilePath /$dirPath") {
                    _isLoading.value = false
                    onSuccess()
                }
            } catch (e: Exception) {
                _error.value = "Error pushing file: ${e.message}"
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
} 