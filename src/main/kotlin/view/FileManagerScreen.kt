package view

import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch
import view.components.CreateDirectoryDialog
import view.components.CreateFileDialog
import view.components.FileEditDialog
import view.components.FileListItem
import view.components.FileManagerToolbar
import view.components.PathNavigator
import view.components.SearchDialog
import view.components.dnd.adbDndSource
import view.components.dnd.adbDndTarget
import view.components.isEditableFile
import view.theme.AdbFileManagerTheme
import viewmodel.DeviceViewModel
import viewmodel.FileManagerViewModel
import viewmodel.ViewMode

/**
 * 文件管理器主屏幕
 */
@Composable
fun FileManagerScreen(deviceViewModel: DeviceViewModel, viewModel: FileManagerViewModel) {
    val scope = rememberCoroutineScope()
    val files by viewModel.files.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val searchResults by viewModel.searchResults.collectAsState(initial = emptyList())
    val isSearching by viewModel.isSearching.collectAsState(initial = false)
    val viewMode = viewModel.viewMode.value

    // 本地UI状态
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showFileEditDialog by remember { mutableStateOf(false) }
    var showFilePicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    // 列表状态，用于滚动相关功能
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val showScrollToTop by remember {
        derivedStateOf {
            if (viewMode == ViewMode.LIST) {
                listState.firstVisibleItemIndex > 5
            } else {
                gridState.firstVisibleItemIndex > 5
            }
        }
    }

    // 重置滚动位置
    fun resetScroll() {
        scope.launch {
            if (viewMode == ViewMode.LIST) {
                listState.animateScrollToItem(0)
            } else {
                gridState.animateScrollToItem(0)
            }
        }
    }

    // 监听目录变化和排序变化，重置滚动位置
    LaunchedEffect(viewModel.directoryPath, viewModel.sortTrigger.collectAsState().value) {
        resetScroll()
    }

    // 首次渲染时加载文件
    LaunchedEffect(Unit) {
        viewModel.loadFiles()
    }

    AdbFileManagerTheme {
        // 整个屏幕容器
        Scaffold(
            // 移除nestedScroll修饰符
            // 移除topBar部分
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 顶部导航区域（包含路径导航和工具栏）
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                            ),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 路径导航区域
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // 路径导航组件，占用主要空间
                                    PathNavigator(
                                        currentPath = viewModel.directoryPath,
                                        onPathClick = { index ->
                                            if (index == -1) {
                                                // 点击根目录
                                                viewModel.directoryPath.clear()
                                                viewModel.loadFiles()
                                            } else {
                                                // 点击路径项
                                                viewModel.navigateToPathIndex(index)
                                            }
                                        },
                                        onPathInput = { path ->
                                            // 处理路径输入
                                            try {
                                                val newPath = path.trim()
                                                if (newPath.isNotEmpty()) {
                                                    // 将输入的路径转换为路径列表
                                                    val pathSegments = newPath.split("/")
                                                        .filter { it.isNotEmpty() }
                                                    if (pathSegments.isNotEmpty()) {
                                                        // 更新当前路径
                                                        viewModel.directoryPath.clear()
                                                        viewModel.directoryPath.addAll(pathSegments)
                                                        // 刷新文件列表
                                                        viewModel.loadFiles()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // 处理路径输入错误
                                                viewModel.setError("无效的路径")
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    // 搜索按钮放在路径导航旁边
                                    IconButton(
                                        onClick = { showSearch = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = "搜索文件",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // 分隔线
                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // 工具栏
                            FileManagerToolbar(
                                deviceViewModel = deviceViewModel,
                                viewModel = viewModel,
                                onCreateDirectoryClick = { showCreateDirDialog = true },
                                onCreateFileClick = { showCreateFileDialog = true },
                                onRefreshClick = { viewModel.reload() },
                                onBackClick = { viewModel.navigateUp() },
                                canNavigateUp = viewModel.canNavigateUp(),
                                onImportClick = { showFilePicker = true },
                                onImportFolderClick = { showFolderPicker = true },
                                onSortTypeChange = { sortType -> viewModel.setSortType(sortType) },
                                currentSortType = viewModel.sortType.collectAsState().value,
                                onViewModeChange = { mode -> viewModel.setViewMode(mode) },
                                currentViewMode = viewMode
                            )
                        }
                    }

                    // 错误消息
                    Box {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = viewModel.error.collectAsState().value != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            viewModel.error.collectAsState().value?.let {
                                val isPermissionError = it.contains("权限不足")

                                Surface(
                                    color = if (isPermissionError)
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                                    else
                                        MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .shadow(
                                            elevation = if (isPermissionError) 4.dp else 1.dp,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isPermissionError)
                                                Icons.Outlined.Error
                                            else
                                                Icons.Outlined.Error,
                                            contentDescription = null,
                                            tint = if (isPermissionError)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = it,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isPermissionError) FontWeight.Medium else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 成功消息
                    Box {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = viewModel.success.collectAsState().value != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            viewModel.success.collectAsState().value?.let {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .shadow(
                                            elevation = 1.dp,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = it,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 文件列表
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .adbDndTarget { files ->
                                viewModel.importFiles(files) {
                                    viewModel.reload()
                                }
                            }
                    ) {
                        if (files.isEmpty() && !isLoading) {
                            // 空状态
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "当前目录为空",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "没有找到任何文件或文件夹",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            // 根据视图模式选择不同的布局
                            when (viewMode) {
                                ViewMode.LIST -> {
                                    // 列表视图
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        contentPadding = PaddingValues(
                                            top = 8.dp,
                                            bottom = 80.dp,
                                            start = 4.dp,
                                            end = 4.dp
                                        )
                                    ) {
                                        items(
                                            items = files,
                                            key = { it.fileName }
                                        ) { file ->
                                            var showDirectoryPicker by remember {
                                                mutableStateOf(
                                                    false
                                                )
                                            }
                                            if (showDirectoryPicker) {
                                                scope.launch {
                                                    val directory = FileKit.openDirectoryPicker()
                                                    showDirectoryPicker = false
                                                    directory ?: return@launch
                                                    viewModel.pullFile(
                                                        file.fileName,
                                                        directory.path
                                                    ) {
                                                        // Ignore
                                                    }

                                                }

                                            }

                                            // 使用带动画的包装器
                                            Box {
                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible = true,
                                                    enter = fadeIn() + expandVertically(),
                                                    exit = fadeOut() + shrinkVertically()
                                                ) {
                                                    FileListItem(
                                                        file = file,
                                                        onFileClick = {
                                                            if (file.isDir) {
                                                                // 如果是目录，则导航到该目录
                                                                // 对于软链接目录，使用link属性作为导航目标（如果有）
                                                                if (file.link != null) {
                                                                    viewModel.navigateTo(file.link)
                                                                } else {
                                                                    viewModel.navigateTo(file.fileName)
                                                                }
                                                            } else {
                                                                // 如果是文件且可编辑，加载文件内容
                                                                if (isEditableFile(file.fileName)) {
                                                                    viewModel.loadFileContent(file.fileName) {
                                                                        showFileEditDialog = true
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onEditFile = {
                                                            viewModel.loadFileContent(file.fileName) {
                                                                showFileEditDialog = true
                                                            }
                                                        },
                                                        onDeleteFile = {
                                                            viewModel.deleteFile(file.fileName) {
                                                                viewModel.loadFiles()
                                                            }
                                                        },
                                                        onDownloadFile = {
                                                            showDirectoryPicker = true
                                                        },
                                                        modifier = Modifier.adbDndSource(
                                                            "/${
                                                                viewModel.directoryPath.joinToString(
                                                                    "/"
                                                                )
                                                            }/${file.fileName}"
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                ViewMode.GRID -> {
                                    // 网格视图
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 120.dp),
                                        state = gridState,
                                        modifier = Modifier.fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        contentPadding = PaddingValues(
                                            top = 16.dp,
                                            bottom = 80.dp,
                                            start = 8.dp,
                                            end = 8.dp
                                        )
                                    ) {
                                        items(
                                            items = files,
                                            key = { it.fileName }
                                        ) { file ->
                                            var showDirectoryPicker by remember {
                                                mutableStateOf(
                                                    false
                                                )
                                            }
                                            if (showDirectoryPicker) {
                                                scope.launch {
                                                    val directory = FileKit.openDirectoryPicker()
                                                    showDirectoryPicker = false
                                                    directory ?: return@launch
                                                    viewModel.pullFile(
                                                        file.fileName,
                                                        directory.path
                                                    ) {
                                                        // 拉取完成
                                                    }

                                                }
                                            }

                                            // 使用带动画的包装器
                                            Box {
                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible = true,
                                                    enter = fadeIn() + expandVertically(),
                                                    exit = fadeOut() + shrinkVertically()
                                                ) {
                                                    GridFileItem(
                                                        file = file,
                                                        onFileClick = {
                                                            if (file.isDir) {
                                                                if (file.link != null) {
                                                                    viewModel.navigateTo(file.link)
                                                                } else {
                                                                    viewModel.navigateTo(file.fileName)
                                                                }
                                                            } else if (isEditableFile(file.fileName)) {
                                                                viewModel.loadFileContent(file.fileName) {
                                                                    showFileEditDialog = true
                                                                }
                                                            }
                                                        },
                                                        onEditFile = {
                                                            viewModel.loadFileContent(file.fileName) {
                                                                showFileEditDialog = true
                                                            }
                                                        },
                                                        onDeleteFile = {
                                                            viewModel.deleteFile(file.fileName) {
                                                                viewModel.loadFiles()
                                                            }
                                                        },
                                                        onDownloadFile = {
                                                            showDirectoryPicker = true
                                                        },
                                                        modifier = Modifier.adbDndSource(
                                                            "/${
                                                                viewModel.directoryPath.joinToString(
                                                                    "/"
                                                                )
                                                            }/${file.fileName}"
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 加载指示器
                        Box {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isLoading,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 8.dp,
                                        shadowElevation = 8.dp,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 4.dp
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "正在加载...",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 回到顶部按钮
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showScrollToTop,
                            enter = fadeIn() + expandIn(expandFrom = Alignment.BottomEnd),
                            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.BottomEnd),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    resetScroll()
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = "返回顶部"
                                )
                            }
                        }
                    }
                }

                // 创建目录对话框
                CreateDirectoryDialog(
                    visible = showCreateDirDialog,
                    onDismissRequest = { showCreateDirDialog = false },
                    onConfirm = { dirName ->
                        viewModel.createDirectory(
                            dirName
                        ) {
                            // 刷新文件列表并关闭对话框
                            viewModel.reload()
                        }
                        showCreateDirDialog = false
                    }
                )

                // 创建文件对话框
                CreateFileDialog(
                    visible = showCreateFileDialog,
                    onDismissRequest = { showCreateFileDialog = false },
                    onConfirm = { fileName, content ->
                        viewModel.createFile(
                            fileName,
                            content
                        ) {
                            // 刷新文件列表并关闭对话框
                            viewModel.reload()
                        }
                        showCreateFileDialog = false
                    }
                )


                // 文件编辑对话框
                FileEditDialog(
                    visible = showFileEditDialog,
                    fileName = viewModel.currentFileName.value,
                    initialContent = viewModel.currentFileContent.value,
                    onDismiss = { showFileEditDialog = false },
                    onSave = { content ->
                        viewModel.saveFileContent(content) {
                            showFileEditDialog = false
                        }
                    },
                    fileEncoding = viewModel.currentFileEncoding.value,
                    onEncodingChange = { encoding ->
                        viewModel.currentFileEncoding.value = encoding
                        // 重新加载文件内容，但不使用自动检测的编码
                        viewModel.loadFileContent(
                            fileName = viewModel.currentFileName.value,
                            useDetectedEncoding = false
                        ) {
                            // 重新加载完成后不需要关闭对话框
                        }
                    }
                )

                if (showFilePicker) {
                    scope.launch {
                        val file = FileKit.openFilePicker()
                        showFilePicker = false
                        file ?: return@launch
                        viewModel.importFile(file.file) {
                            viewModel.reload()
                        }
                    }
                }

                if (showFolderPicker) {
                    scope.launch {
                        val directory = FileKit.openDirectoryPicker()
                        showFolderPicker = false
                        directory ?: return@launch
                        viewModel.importFolder(directory.path) {
                            viewModel.reload()
                        }

                    }
                }

                // 搜索对话框
                SearchDialog(
                    viewModel = viewModel,
                    visible = showSearch,
                    onDismiss = { showSearch = false },
                    onFileClick = { file ->
                        if (file.isDir) {
                            viewModel.navigateTo(file.fileName)
                        } else if (isEditableFile(file.fileName)) {
                            viewModel.loadFileContent(file.fileName) {
                                showFileEditDialog = true
                            }
                        }
                        showSearch = false
                    },
                    onEditFile = { file ->
                        viewModel.loadFileContent(file.fileName) {
                            showFileEditDialog = true
                        }
                        showSearch = false
                    },
                    onDeleteFile = { file ->
                        viewModel.deleteFile(file.fileName) {
                            viewModel.loadFiles()
                        }
                        showSearch = false
                    },
                    searchResults = searchResults,
                    onSearch = { query ->
                        viewModel.searchFiles(query)
                    },
                    isLoading = isSearching
                )
            }
        }
    }
}

@Composable
fun GridFileItem(
    file: model.FileItem,
    onFileClick: () -> Unit,
    onEditFile: () -> Unit,
    onDeleteFile: () -> Unit,
    onDownloadFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var startPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingEvent by remember { mutableStateOf({}) }
    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onPointerEvent(PointerEventType.Press) {
                    startPosition = it.changes.first().position
                    pendingEvent = when {
                        it.buttons.isPrimaryPressed -> onFileClick
                        it.buttons.isSecondaryPressed -> {
                            { showContextMenu = true }
                        }

                        else -> {
                            {}
                        }
                    }
                }
                .onPointerEvent(PointerEventType.Release) {
                    val currentPosition = it.changes.first().position
                    val diffX = startPosition.x - currentPosition.x
                    val diffY = startPosition.y - currentPosition.y

                    if (diffX * diffX + diffY * diffY > 5 * 5) {
                        println("pointer moved")
                        startPosition = Offset.Zero
                        pendingEvent = {}
                        return@onPointerEvent
                    }
                    pendingEvent()
                    startPosition = Offset.Zero
                    pendingEvent = {}
                }
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (file.isDir) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (file.isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = file.fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
        // 右键菜单
        if (showContextMenu) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { showContextMenu = false },
                offset = DpOffset(
                    x = 0.dp,
                    y = 0.dp
                )
            ) {
                if (!file.isDir && isEditableFile(file.fileName)) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            onEditFile()
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("下载") },
                    onClick = {
                        onDownloadFile()
                        showContextMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        onDeleteFile()
                        showContextMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}