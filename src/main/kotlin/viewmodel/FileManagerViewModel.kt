package viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.AppInfo
import model.Bookmark
import model.FileItem
import model.FileTransferProgress
import model.FileTransferState
import model.FileUtils
import model.SortType
import model.StringsManager
import model.LocalDestinationException
import model.LocalDestinationFailure
import model.validateLocalDestinationDirectory
import model.encodeTextLosslessly
import model.normalizeDetectedTextEncoding
import org.mozilla.universalchardet.UniversalDetector
import data.remote.adb.AdbDevicePoller
import data.repository.BookmarkRepository
import view.LoadState
import view.effect.FileManagerEffect
import view.intent.FileManagerIntent
import view.state.FileManagerState
import java.io.File
import java.nio.charset.Charset

class FileManagerViewModel(
    private val adbDevicePoller: AdbDevicePoller,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FileManagerState())
    val state: StateFlow<FileManagerState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<FileManagerEffect>(replay = 1)
    val effect: SharedFlow<FileManagerEffect> = _effect.asSharedFlow()

    private val _directoryList = mutableStateListOf<String>()
    val directoryList: SnapshotStateList<String> = _directoryList

    private val _transferProgress = FileTransferProgress()
    val transferProgress: StateFlow<FileTransferState> = _transferProgress.state
    private var transferJob: Job? = null

    private val directoryPath get() = _directoryList.joinToString("/")
    val currentDirectoryPath get() = if (directoryPath.isEmpty()) "/" else "/$directoryPath"

    init {
        loadFiles()
        observeBookmarks()
    }

    private fun observeBookmarks() {
        bookmarkRepository.bookmarks.onEach { bookmarks ->
            _state.update {
                it.copy(
                    bookmarks = bookmarks,
                    currentPathBookmarked = bookmarkRepository.isBookmarked("/$directoryPath")
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun checkCurrentPathBookmarked() {
        viewModelScope.launch {
            val isBookmarked = bookmarkRepository.isBookmarked("/$directoryPath")
            _state.update { it.copy(currentPathBookmarked = isBookmarked) }
        }
    }

    fun dispatch(intent: FileManagerIntent) {
        when (intent) {
            is FileManagerIntent.ResetDirectory -> resetDirectory(intent.path)
            FileManagerIntent.NavigateUp -> navigateUp()
            is FileManagerIntent.NavigateToPathIndex -> navigateToPathIndex(intent.index)
            FileManagerIntent.LoadFiles -> loadFiles()
            is FileManagerIntent.ImportFiles -> importFiles(intent.files)
            is FileManagerIntent.DeleteFile -> deleteFile(intent.fileName)
            is FileManagerIntent.RequestDeleteConfirmation -> {
                _state.update { it.copy(pendingDeleteFile = intent.fileName) }
            }

            FileManagerIntent.CancelDeleteConfirmation -> {
                _state.update { it.copy(pendingDeleteFile = null) }
            }

            is FileManagerIntent.NavigateTo -> navigateTo(intent.directoryName)
            is FileManagerIntent.ResetSortType -> setSortType(intent.type)
            is FileManagerIntent.ExportFile -> exportFile(
                intent.fileName,
                intent.destinationPath,
                intent.deleteRemoteAfterExport
            )
            is FileManagerIntent.LoadFileContent -> loadFileContent(
                intent.fileName,
                intent.useDetectedEncoding
            )

            is FileManagerIntent.ChangeEncoding -> changeFileEncoding(intent.encoding)
            FileManagerIntent.CloseEditor -> _state.update { it.copy(fileContent = null, fileName = null) }
            is FileManagerIntent.SaveFile -> saveFile(intent.newContent)
            is FileManagerIntent.RequestRename -> {
                _state.update { it.copy(pendingRenameFile = intent.file) }
            }
            FileManagerIntent.DismissRenameDialog -> {
                _state.update { it.copy(pendingRenameFile = null) }
            }
            is FileManagerIntent.RenameFile -> renameFile(intent.oldName, intent.newName)
            FileManagerIntent.ShowCreateDirectoryDialog -> {
                _state.update { it.copy(showCreateDirectoryDialog = true) }
            }
            FileManagerIntent.ShowCreateFileDialog -> {
                _state.update { it.copy(showCreateFileDialog = true) }
            }
            FileManagerIntent.DismissCreateDialogs -> {
                _state.update { it.copy(showCreateDirectoryDialog = false, showCreateFileDialog = false) }
            }
            is FileManagerIntent.CreateDirectory -> createDirectory(intent.dirName)
            is FileManagerIntent.CreateFile -> createFile(intent.fileName, intent.content)
            FileManagerIntent.RequestBookmarkMenu -> {
                _state.update { it.copy(showBookmarkMenu = true) }
            }
            FileManagerIntent.DismissBookmarkMenu -> {
                _state.update { it.copy(showBookmarkMenu = false) }
            }
            FileManagerIntent.RequestAddBookmarkDialog -> {
                _state.update { it.copy(showAddBookmarkDialog = true) }
            }
            FileManagerIntent.DismissAddBookmarkDialog -> {
                _state.update { it.copy(showAddBookmarkDialog = false) }
            }
            is FileManagerIntent.AddBookmark -> addBookmark(intent.name)
            is FileManagerIntent.RemoveBookmark -> removeBookmark(intent.bookmark)
            is FileManagerIntent.NavigateToBookmark -> navigateToBookmark(intent.bookmark)
            is FileManagerIntent.RequestChangePermissions -> {
                _state.update { it.copy(pendingChangePermissionsFile = intent.file, showChangePermissionsDialog = true) }
            }
            FileManagerIntent.DismissChangePermissionsDialog -> {
                _state.update { it.copy(pendingChangePermissionsFile = null, showChangePermissionsDialog = false) }
            }
            is FileManagerIntent.ChangePermissions -> changePermissions(intent.fileName, intent.permissions)
            is FileManagerIntent.StartTransfer -> {
                _state.update { it.copy(isTransferring = true, transferringFileName = intent.fileName, showTransferDialog = true) }
            }
            FileManagerIntent.EndTransfer -> {
                cancelActiveTransfer()
            }
            is FileManagerIntent.InstallApk -> installApk(intent.fileName)
            is FileManagerIntent.RequestCopy -> {
                _state.update { it.copy(pendingCopyMoveFile = intent.file, isCopyOperation = true, showCopyMoveDialog = true) }
            }
            is FileManagerIntent.RequestMove -> {
                _state.update { it.copy(pendingCopyMoveFile = intent.file, isCopyOperation = false, showCopyMoveDialog = true) }
            }
            FileManagerIntent.DismissCopyMoveDialog -> {
                _state.update { it.copy(pendingCopyMoveFile = null, showCopyMoveDialog = false) }
            }
            is FileManagerIntent.CopyFile -> copyFile(intent.fileName, intent.destPath)
            is FileManagerIntent.MoveFile -> moveFile(intent.fileName, intent.destPath)
            FileManagerIntent.ToggleSelectionMode -> toggleSelectionMode()
            is FileManagerIntent.ToggleFileSelection -> toggleFileSelection(intent.fileName)
            FileManagerIntent.SelectAllFiles -> selectAllFiles()
            FileManagerIntent.ClearFileSelection -> clearFileSelection()
            FileManagerIntent.RequestBatchDeleteConfirmation -> requestBatchDeleteConfirmation()
            FileManagerIntent.CancelBatchDeleteConfirmation -> _state.update {
                it.copy(pendingBatchDeleteFiles = emptySet())
            }
            FileManagerIntent.BatchDeleteFiles -> batchDeleteFiles()
            is FileManagerIntent.BatchExportFiles -> batchExportFiles(intent.destinationPath)
            is FileManagerIntent.BatchMoveToLocal -> batchExportFiles(intent.destinationPath, deleteRemoteAfterExport = true)
            is FileManagerIntent.BatchCopyTo -> batchCopyMoveTo(intent.destinationPath, move = false)
            is FileManagerIntent.BatchMoveTo -> batchCopyMoveTo(intent.destinationPath, move = true)
            FileManagerIntent.ShowAppManager -> showAppManager()
            FileManagerIntent.DismissAppManager -> _state.update { it.copy(showAppManager = false) }
            is FileManagerIntent.UninstallApp -> uninstallApp(intent.packageName)
            is FileManagerIntent.BackupApk -> backupApk(intent.packageName, intent.destinationPath)
            FileManagerIntent.TakeScreenshot -> takeScreenshot()
            FileManagerIntent.ShowClipboardDialog -> _state.update { it.copy(showClipboardDialog = true) }
            FileManagerIntent.DismissClipboardDialog -> _state.update { it.copy(showClipboardDialog = false) }
            is FileManagerIntent.PushClipboard -> pushClipboard(intent.text)
        }
    }

    private fun changePermissions(fileName: String, permissions: String) {
        _state.update { it.copy(operationInProgress = true, showChangePermissionsDialog = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.changePermissions(directoryPath, fileName, permissions)
                // 添加延迟，确保权限修改生效
                kotlinx.coroutines.delay(500)
                loadFiles()
                _effect.emit(FileManagerEffect.ShowSuccessTips(StringsManager.strings.value.permissionChangeSuccess))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.permissionChangeFailed(e.message ?: "")))
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    private fun addBookmark(name: String) {
        viewModelScope.launch {
            try {
                bookmarkRepository.addBookmark(name, "/$directoryPath")
                _state.update { it.copy(showAddBookmarkDialog = false) }
                _effect.emit(FileManagerEffect.ShowSuccessTips(StringsManager.strings.value.bookmarkAddedSuccess))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.bookmarkAddedFailed(e.message ?: "")))
            }
        }
    }

    private fun removeBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            try {
                bookmarkRepository.removeBookmark(bookmark)
                _effect.emit(FileManagerEffect.ShowSuccessTips(StringsManager.strings.value.bookmarkRemovedSuccess))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.bookmarkRemovedFailed(e.message ?: "")))
            }
        }
    }

    private fun navigateToBookmark(bookmark: Bookmark) {
        _state.update { it.copy(showBookmarkMenu = false) }
        resetDirectory(bookmark.path.removePrefix("/"))
    }

    private fun setSortType(type: SortType) {
        _state.update { it.copy(sortType = type) }
        loadFiles()
    }


    private fun resetDirectory(directoryPath: String) {
        viewModelScope.launch {
            try {
                val newPath = directoryPath.trim()
                if (newPath.isNotEmpty()) {
                    val hasPermission = withContext(Dispatchers.IO) {
                        adbDevicePoller.checkPermission(newPath)
                    }
                    if (!hasPermission) {
                        _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.pathInvalid(newPath)))
                        return@launch
                    }
                    val pathSegments = newPath.split("/")
                        .filter { it.isNotEmpty() }

                    if (pathSegments.isNotEmpty()) {
                        _directoryList.clear()
                        _directoryList.addAll(pathSegments)
                        loadFiles()
                        checkCurrentPathBookmarked()
                    }
                }
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.pathInvalid(e.message ?: "")))
            }
        }
    }

    fun loadFileContent(
        fileName: String,
        useDetectedEncoding: Boolean
    ) {
        val fileSize = (_state.value.files as? LoadState.Success)?.data
            ?.find { it.fileName == fileName }?.sizeBytes ?: 0L
        _transferProgress.startTransfer(fileName, fileSize)
        _state.update { it.copy(operationInProgress = true, isTransferring = true, transferringFileName = fileName, showTransferDialog = true) }
        val tempFile = File.createTempFile("pull_", ".tmp")
        launchTransfer {
            try {
                val filePath = "${directoryPath}/${fileName}"
                adbDevicePoller.pullWithProgress(
                    filePath,
                    tempFile.absolutePath,
                    data.remote.adb.AdbTransferProgress { percent ->
                        _transferProgress.updateProgress((percent / 100f * fileSize).toLong())
                    }
                ).collect { /* consume flow to drive pull completion */ }
                _transferProgress.complete()
                val encoding = if (useDetectedEncoding) {
                    detectEncoding(tempFile)
                } else {
                    _state.value.fileEncoding
                }
                val content = tempFile.readText(Charset.forName(encoding))
                _state.update { it.copy(fileName = fileName, fileEncoding = encoding, fileContent = content) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transferProgress.fail(e.message.toString())
                _effect.emit(FileManagerEffect.ShowErrorTips(e.message.toString()))
            } finally {
                _state.update { it.copy(operationInProgress = false, isTransferring = false, transferringFileName = null, showTransferDialog = false) }
                tempFile.delete()
            }
        }
    }

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
        return normalizeDetectedTextEncoding(detector.detectedCharset)
    }

    private fun importFiles(files: List<File>) {
        _state.update { it.copy(operationInProgress = true) }
        launchTransfer {
            try {
                for (file in files) {
                    val destPath = if (file.isDirectory) {
                        "${directoryPath}/${file.name}"
                    } else {
                        "${directoryPath}/${file.name}"
                    }
                    val fileSize = file.length()
                    _transferProgress.startTransfer(file.name, fileSize)
                    _state.update { it.copy(isTransferring = true, transferringFileName = file.name, showTransferDialog = true) }

                    if (file.isFile) {
                        // Use pushWithProgress for real progress tracking
                        adbDevicePoller.pushWithProgress(
                            file.canonicalPath,
                            destPath
                        ) { percent ->
                            _transferProgress.updateProgress((percent / 100f * fileSize).toLong())
                        }.collect { /* consume flow to drive push completion */ }
                    } else {
                        adbDevicePoller.push(file.canonicalPath, destPath)
                    }
                    _transferProgress.complete()
                }
                loadFiles()
                _effect.emit(FileManagerEffect.ShowSuccessTips(StringsManager.strings.value.fileImportSuccess))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transferProgress.fail(e.message ?: "Transfer failed")
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.fileImportFailed(e.message ?: "")))
            } finally {
                _state.update { it.copy(operationInProgress = false, isTransferring = false, transferringFileName = null, showTransferDialog = false) }
            }
        }
    }

    private fun exportFile(
        fileName: String,
        destinationPath: String,
        deleteRemoteAfterExport: Boolean = false
    ) {
        val fileSize = (_state.value.files as? LoadState.Success)?.data
            ?.find { it.fileName == fileName }?.sizeBytes ?: 0L
        _transferProgress.startTransfer(fileName, fileSize)
        _state.update { it.copy(operationInProgress = true, isTransferring = true, transferringFileName = fileName, showTransferDialog = true) }
        val destination = File(destinationPath).absoluteFile
        val destinationExistedBefore = destination.exists()
        launchTransfer {
            try {
                val destinationDirectory = destination.parentFile
                    ?: throw LocalDestinationException(LocalDestinationFailure.NOT_DIRECTORY, destination)
                validateLocalDestinationDirectory(destinationDirectory)
                adbDevicePoller.pullWithProgress(
                    "${directoryPath}/${fileName}",
                    destinationPath
                ) { percent ->
                    _transferProgress.updateProgress((percent / 100f * fileSize).toLong())
                }.collect { /* consume flow to drive pull completion */ }
                _transferProgress.complete()
                if (deleteRemoteAfterExport) {
                    adbDevicePoller.delete("${directoryPath}/${fileName}")
                    loadFiles()
                }
                _effect.emit(FileManagerEffect.LocalFilesChanged)
            } catch (e: CancellationException) {
                if (!destinationExistedBefore) destination.deleteRecursively()
                throw e
            } catch (e: Exception) {
                _transferProgress.fail(e.message ?: "Export failed")
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.fileExportFailed(exportErrorMessage(e))))
            } finally {
                _state.update { it.copy(operationInProgress = false, isTransferring = false, transferringFileName = null, showTransferDialog = false) }
            }
        }
    }

    /**
     * Synchronous version of exportFile for DND (Drag and Drop).
     * Called from PlaceholderTransferable.getTransferData() via runBlocking,
     * running on the AWT DnD thread (not Compose main thread).
     */
    suspend fun exportFileSync(fileName: String, destinationPath: String) {
        File(destinationPath).absoluteFile.parentFile?.let(::validateLocalDestinationDirectory)
        adbDevicePoller.pull("${directoryPath}/${fileName}", destinationPath)
    }

    private fun launchTransfer(block: suspend CoroutineScope.() -> Unit) {
        transferJob?.cancel()
        val job = viewModelScope.launch(Dispatchers.IO, block = block)
        transferJob = job
        job.invokeOnCompletion {
            if (transferJob === job) transferJob = null
        }
    }

    private fun cancelActiveTransfer() {
        transferJob?.cancel(CancellationException("Transfer cancelled by user"))
        _transferProgress.reset()
        _state.update {
            it.copy(
                operationInProgress = false,
                isTransferring = false,
                transferringFileName = null,
                showTransferDialog = false
            )
        }
    }

    private fun exportErrorMessage(error: Exception): String {
        if (error !is LocalDestinationException) return error.message.orEmpty()
        val path = error.directory.absolutePath
        return when (error.failure) {
            LocalDestinationFailure.NOT_FOUND -> StringsManager.strings.value.localDirectoryNotFound(path)
            LocalDestinationFailure.NOT_DIRECTORY -> StringsManager.strings.value.localDestinationNotDirectory(path)
            LocalDestinationFailure.NOT_WRITABLE -> StringsManager.strings.value.localDirectoryNotWritable(path)
        }
    }

    private fun deleteFile(fileName: String) {
        _state.update { it.copy(operationInProgress = true, pendingDeleteFile = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.delete("${directoryPath}/${fileName}")
                loadFiles()
                _effect.emit(FileManagerEffect.ShowSuccessTips(StringsManager.strings.value.fileDeleteSuccess))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.fileDeleteFailed(e.message ?: "")))
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    private fun navigateTo(directoryName: String) {
        println("navigateTo called with: '$directoryName', current directoryList: $_directoryList")
        val isSymlink = directoryName.contains("/") || directoryName.contains("\\")

        if (isSymlink) {
            if (directoryName.startsWith("/")) {
                _directoryList.clear()
                val pathComponents =
                    directoryName.substring(1).split("/").filter { it.isNotEmpty() }
                _directoryList.addAll(pathComponents)
            } else {
                val pathComponents = directoryName.split("/").filter { it.isNotEmpty() }

                for (component in pathComponents) {
                    when (component) {
                        "." -> continue
                        ".." -> {
                            removeLastDirectory()
                        }

                        else -> _directoryList.add(component)
                    }
                }
            }
        } else {
            _directoryList.add(directoryName)
        }

        println("After navigateTo, directoryList: $_directoryList, directoryPath: '$directoryPath'")
        loadFiles()
        checkCurrentPathBookmarked()
    }

    private fun navigateToPathIndex(index: Int) {
        if (index < -1 || index >= _directoryList.size) {
            viewModelScope.launch {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.pathInvalidIndex))
            }
            return
        }
        if (index == -1) {
            _directoryList.clear()
        } else {
            while (_directoryList.size > index + 1) {
                if (!removeLastDirectory()) break
            }
        }
        loadFiles()
        checkCurrentPathBookmarked()
    }

    private fun navigateUp() {
        if (removeLastDirectory()) {
            loadFiles()
            checkCurrentPathBookmarked()
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.pathAlreadyRoot))
            }
        }
    }

    private fun changeFileEncoding(encoding: String) {
        if (!Charset.isSupported(encoding)) {
            viewModelScope.launch {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.editorUnsupportedEncoding(encoding)))
            }
            return
        }
        val currentFileName = _state.value.fileName ?: return
        _state.update { it.copy(fileEncoding = encoding, operationInProgress = true) }
        val tempFile = File.createTempFile("redecode_", ".tmp")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.pull("${directoryPath}/${currentFileName}", tempFile.absolutePath)
                val content = tempFile.readText(Charset.forName(encoding))
                _state.update { it.copy(fileContent = content) }
            } catch (e: Exception) {
                _effect.emit(
                    FileManagerEffect.ShowErrorTips(
                        StringsManager.strings.value.editorReloadEncodingFailed(encoding, e.message.orEmpty())
                    )
                )
            } finally {
                tempFile.delete()
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    private fun removeLastDirectory(): Boolean {
        val lastIndex = _directoryList.lastIndex
        if (lastIndex < 0) return false
        _directoryList.removeAt(lastIndex)
        return true
    }

    private fun loadFiles() {
        println("Loading files for path: '$directoryPath', directoryList: $_directoryList")
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(files = LoadState.Loading) }
            try {
                val result = adbDevicePoller.loadFiles(directoryPath)
                println("Load files result: $result")
                val list = FileUtils.parseStatOutput(result)
                if (list.isEmpty()) {
                    _state.update { it.copy(files = LoadState.Empty) }
                } else {
                    _state.update { state ->
                        state.copy(files = LoadState.Success(list.toMutableList().apply {
                            when (state.sortType) {
                                SortType.NAME_ASC -> sortBy { it.fileName.lowercase() }
                                SortType.NAME_DESC -> sortByDescending { it.fileName.lowercase() }
                                SortType.TYPE_ASC -> sortWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.fileName.lowercase() })
                                SortType.TYPE_DESC -> sortWith(compareBy<FileItem> { it.isDir }.thenByDescending { it.fileName.lowercase() })
                                SortType.DATE_ASC -> sortBy { it.modifiedEpochSeconds }
                                SortType.DATE_DESC -> sortByDescending { it.modifiedEpochSeconds }
                                SortType.SIZE_ASC -> sortBy { it.sizeBytes }
                                SortType.SIZE_DESC -> sortByDescending { it.sizeBytes }
                            }
                        }))
                    }
                }
            } catch (e: Exception) {
                println("Error loading files: ${e.message}")
                e.printStackTrace()
                _state.update { it.copy(files = LoadState.Error(e)) }
            }
        }
    }

    private fun saveFile(newContent: String) {
        val currentFileName = _state.value.fileName ?: return
        _state.update { it.copy(operationInProgress = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File.createTempFile("save_", ".tmp")
                try {
                    val originalEncoding = _state.value.fileEncoding
                    val encoded = encodeTextLosslessly(newContent, originalEncoding)
                    tempFile.writeBytes(encoded.bytes)
                    adbDevicePoller.push(tempFile.absolutePath, "${directoryPath}/${currentFileName}")
                    _state.update {
                        it.copy(fileContent = newContent, fileEncoding = encoded.encoding)
                    }
                    val message = if (encoded.upgradedToUtf8) {
                        StringsManager.strings.value.editorSavedAsUtf8(originalEncoding)
                    } else {
                        StringsManager.strings.value.fileSaveSuccess
                    }
                    _effect.emit(FileManagerEffect.ShowSuccessTips(message))
                } finally {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.fileSaveFailed(e.message ?: "")))
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    private fun createDirectory(dirName: String) {
        _state.update { it.copy(operationInProgress = true, showCreateDirectoryDialog = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.createDirectory(directoryPath, dirName)
                loadFiles()
                _effect.emit(FileManagerEffect.ShowSuccessTips(StringsManager.strings.value.folderCreatedSuccess))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.folderCreatedFailed(e.message ?: "")))
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    private fun createFile(fileName: String, content: String) {
        _state.update { it.copy(operationInProgress = true, showCreateFileDialog = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.createFile(directoryPath, fileName, content)
                loadFiles()
                _effect.emit(FileManagerEffect.ShowSuccessTips(StringsManager.strings.value.fileCreatedSuccess))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.fileCreatedFailed(e.message ?: "")))
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    private fun renameFile(oldName: String, newName: String) {
        _state.update { it.copy(operationInProgress = true, pendingRenameFile = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.renameFile(directoryPath, oldName, newName)
                loadFiles()
                _effect.emit(FileManagerEffect.ShowSuccessTips(StringsManager.strings.value.renameSuccess))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.renameFailed(e.message ?: "")))
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    private fun installApk(fileName: String) {
        _state.update { it.copy(operationInProgress = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val filePath = "${directoryPath}/$fileName"
                val result = adbDevicePoller.installApk(filePath)
                _effect.emit(FileManagerEffect.ShowSuccessTips(
                    StringsManager.strings.value.apkInstallSuccess
                ))
                println("APK install result: $result")
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(
                    StringsManager.strings.value.apkInstallFailed(e.message ?: "")
                ))
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    private fun copyFile(fileName: String, destPath: String) {
        _state.update { it.copy(operationInProgress = true, showCopyMoveDialog = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.copyFile("${directoryPath}/$fileName", destPath)
                loadFiles()
                _effect.emit(FileManagerEffect.ShowSuccessTips(StringsManager.strings.value.copySuccess))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.copyFailed(e.message ?: "")))
            } finally {
                _state.update { it.copy(operationInProgress = false, pendingCopyMoveFile = null) }
            }
        }
    }

    private fun moveFile(fileName: String, destPath: String) {
        _state.update { it.copy(operationInProgress = true, showCopyMoveDialog = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.moveFile("${directoryPath}/$fileName", destPath)
                loadFiles()
                _effect.emit(FileManagerEffect.ShowSuccessTips(StringsManager.strings.value.moveSuccess))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(StringsManager.strings.value.moveFailed(e.message ?: "")))
            } finally {
                _state.update { it.copy(operationInProgress = false, pendingCopyMoveFile = null) }
            }
        }
    }

    private fun toggleSelectionMode() {
        _state.update {
            val enabled = !it.selectionMode
            it.copy(
                selectionMode = enabled,
                selectedFiles = if (enabled) it.selectedFiles else emptySet(),
                pendingBatchDeleteFiles = emptySet()
            )
        }
    }

    private fun toggleFileSelection(fileName: String) {
        _state.update { state ->
            val newSelected = if (fileName in state.selectedFiles) {
                state.selectedFiles - fileName
            } else {
                state.selectedFiles + fileName
            }
            state.copy(selectedFiles = newSelected)
        }
    }

    private fun selectAllFiles() {
        val files = _state.value.files
        if (files is LoadState.Success) {
            val allNames = files.data.map { it.fileName }.toSet()
            _state.update { it.copy(selectedFiles = allNames) }
        }
    }

    private fun clearFileSelection() {
        _state.update { it.copy(selectedFiles = emptySet()) }
    }

    private fun requestBatchDeleteConfirmation() {
        val selected = _state.value.selectedFiles
        if (selected.isNotEmpty()) {
            _state.update { it.copy(pendingBatchDeleteFiles = selected) }
        }
    }

    private fun batchDeleteFiles() {
        val files = _state.value.pendingBatchDeleteFiles.toList()
        if (files.isEmpty()) return
        _state.update {
            it.copy(operationInProgress = true, pendingDeleteFile = null, pendingBatchDeleteFiles = emptySet())
        }
        viewModelScope.launch(Dispatchers.IO) {
            val failures = mutableListOf<String>()
            for (fileName in files) {
                try {
                    adbDevicePoller.delete("${directoryPath}/$fileName")
                } catch (_: Exception) {
                    failures += fileName
                }
            }
            try {
                loadFiles()
                if (failures.isEmpty()) {
                    _state.update { it.copy(selectedFiles = emptySet(), selectionMode = false) }
                    _effect.emit(FileManagerEffect.ShowSuccessTips("已删除 ${files.size} 个项目"))
                } else {
                    _state.update { it.copy(selectedFiles = failures.toSet()) }
                    _effect.emit(FileManagerEffect.ShowErrorTips(
                        "已删除 ${files.size - failures.size} 个项目，${failures.size} 个失败：${failures.joinToString()}"
                    ))
                }
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    private fun batchExportFiles(destinationPath: String, deleteRemoteAfterExport: Boolean = false) {
        val files = _state.value.selectedFiles.toList()
        if (files.isEmpty() || destinationPath.isBlank()) return
        val fileMetadata = (_state.value.files as? LoadState.Success)?.data
            ?.associateBy { it.fileName }
            .orEmpty()
        _state.update {
            it.copy(
                operationInProgress = true,
                isTransferring = true,
                transferringFileName = files.firstOrNull(),
                showTransferDialog = true
            )
        }
        launchTransfer {
            val destinationDirectory = File(destinationPath).absoluteFile
            try {
                validateLocalDestinationDirectory(destinationDirectory)
            } catch (error: LocalDestinationException) {
                _effect.emit(
                    FileManagerEffect.ShowErrorTips(
                        StringsManager.strings.value.fileExportFailed(exportErrorMessage(error))
                    )
                )
                _state.update {
                    it.copy(
                        operationInProgress = false,
                        isTransferring = false,
                        transferringFileName = null,
                        showTransferDialog = false
                    )
                }
                return@launchTransfer
            }
            val failures = mutableListOf<String>()
            for (fileName in files) {
                val sizeBytes = fileMetadata[fileName]?.sizeBytes ?: 0L
                _transferProgress.startTransfer(fileName, sizeBytes)
                _state.update { it.copy(transferringFileName = fileName) }
                val destFile = File(destinationDirectory, fileName)
                val destinationExistedBefore = destFile.exists()
                try {
                    val srcPath = "${directoryPath}/$fileName"
                    if (fileMetadata[fileName]?.isDir == true) {
                        adbDevicePoller.pull(srcPath, destFile.absolutePath)
                    } else {
                        adbDevicePoller.pullWithProgress(srcPath, destFile.absolutePath) { percent ->
                            _transferProgress.updateProgress((percent / 100f * sizeBytes).toLong())
                        }.collect { }
                    }
                    if (deleteRemoteAfterExport) {
                        adbDevicePoller.delete(srcPath)
                    }
                    _transferProgress.complete()
                } catch (e: CancellationException) {
                    if (!destinationExistedBefore) destFile.deleteRecursively()
                    throw e
                } catch (e: Exception) {
                    _transferProgress.fail(e.message ?: "导出失败")
                    failures += fileName
                }
            }
            try {
                if (failures.size < files.size) {
                    _effect.emit(FileManagerEffect.LocalFilesChanged)
                }
                if (failures.isEmpty()) {
                    _state.update { it.copy(selectedFiles = emptySet(), selectionMode = false) }
                    _effect.emit(FileManagerEffect.ShowSuccessTips("已导出 ${files.size} 个项目"))
                } else {
                    _state.update { it.copy(selectedFiles = failures.toSet()) }
                    _effect.emit(FileManagerEffect.ShowErrorTips(
                        "已导出 ${files.size - failures.size} 个项目，${failures.size} 个失败：${failures.joinToString()}"
                    ))
                }
            } finally {
                _state.update { it.copy(operationInProgress = false, isTransferring = false, transferringFileName = null, showTransferDialog = false) }
            }
        }
    }

    private fun batchCopyMoveTo(destinationPath: String, move: Boolean) {
        val files = _state.value.selectedFiles.toList()
        if (files.isEmpty() || destinationPath.isBlank()) return
        _state.update { it.copy(operationInProgress = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val failures = mutableListOf<String>()
            for (fileName in files) {
                try {
                    val sourcePath = "${directoryPath}/$fileName"
                    if (move) {
                        adbDevicePoller.moveFile(sourcePath, destinationPath)
                    } else {
                        adbDevicePoller.copyFile(sourcePath, destinationPath)
                    }
                } catch (_: Exception) {
                    failures += fileName
                }
            }
            try {
                loadFiles()
                val completed = files.size - failures.size
                if (failures.isEmpty()) {
                    _state.update { it.copy(selectedFiles = emptySet(), selectionMode = false) }
                    _effect.emit(
                        FileManagerEffect.ShowSuccessTips(
                            if (move) "已移动 $completed 个项目" else "已复制 $completed 个项目"
                        )
                    )
                } else {
                    _state.update { it.copy(selectedFiles = failures.toSet()) }
                    _effect.emit(
                        FileManagerEffect.ShowErrorTips(
                            "已${if (move) "移动" else "复制"} $completed 个项目，${failures.size} 个失败：${failures.joinToString()}"
                        )
                    )
                }
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    // ── App management ──────────────────────────────────────────────

    private fun showAppManager() {
        _state.update { it.copy(showAppManager = true, appListLoading = true, appList = emptyList()) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val packages = adbDevicePoller.getInstalledPackages().sorted()
                _state.update { it.copy(appList = packages.map(::AppInfo)) }
                // Resolve labels in background, one by one
                packages.forEach { pkg ->
                    val label = adbDevicePoller.getAppLabel(pkg)
                    _state.update { state ->
                        val updated = state.appList.map { a ->
                            if (a.packageName == pkg) a.copy(label = label) else a
                        }
                        state.copy(appList = updated)
                    }
                }
                _state.update { it.copy(appListLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(appListLoading = false) }
                _effect.emit(FileManagerEffect.ShowErrorTips(e.message ?: "Failed to load apps"))
            }
        }
    }

    private fun uninstallApp(packageName: String) {
        if (packageName.isBlank()) return
        _state.update { it.copy(operationInProgress = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.uninstallApp(packageName)
                _state.update { it.copy(appList = it.appList.filter { a -> a.packageName != packageName }) }
                _effect.emit(FileManagerEffect.ShowSuccessTips("Uninstalled $packageName"))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(e.message ?: "Uninstall failed"))
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    private fun backupApk(packageName: String, destinationPath: String) {
        if (packageName.isBlank() || destinationPath.isBlank()) return
        _transferProgress.startTransfer("$packageName.apk", 0L)
        _state.update { it.copy(operationInProgress = true, isTransferring = true, transferringFileName = "$packageName.apk", showTransferDialog = true) }
        val destinationExistedBefore = File(destinationPath).exists()
        launchTransfer {
            try {
                adbDevicePoller.backupApk(packageName, destinationPath)
                _transferProgress.complete()
                _effect.emit(FileManagerEffect.ShowSuccessTips("Backup saved"))
            } catch (e: CancellationException) {
                if (!destinationExistedBefore) File(destinationPath).deleteRecursively()
                throw e
            } catch (e: Exception) {
                _transferProgress.fail(e.message ?: "Backup failed")
                _effect.emit(FileManagerEffect.ShowErrorTips(e.message ?: "Backup failed"))
            } finally {
                _state.update { it.copy(operationInProgress = false, isTransferring = false, transferringFileName = null, showTransferDialog = false) }
            }
        }
    }

    // ── Screenshot ──────────────────────────────────────────────────

    private fun takeScreenshot() {
        _state.update { it.copy(operationInProgress = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                val timestamp = sdf.format(java.util.Date())
                val downloadsDir = java.io.File(System.getProperty("user.home"), "Downloads")
                val destFile = java.io.File(downloadsDir, "screenshot_$timestamp.png")
                adbDevicePoller.takeScreenshot(destFile.absolutePath)
                _effect.emit(FileManagerEffect.ShowSuccessTips("Screenshot saved: ${destFile.name}"))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(e.message ?: "Screenshot failed"))
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }

    // ── Clipboard ───────────────────────────────────────────────────

    private fun pushClipboard(text: String) {
        if (text.isBlank()) return
        _state.update { it.copy(operationInProgress = true, showClipboardDialog = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                adbDevicePoller.pushClipboard(text)
                _effect.emit(FileManagerEffect.ShowSuccessTips("Clipboard pushed"))
            } catch (e: Exception) {
                _effect.emit(FileManagerEffect.ShowErrorTips(e.message ?: "Clipboard push failed"))
            } finally {
                _state.update { it.copy(operationInProgress = false) }
            }
        }
    }
}
