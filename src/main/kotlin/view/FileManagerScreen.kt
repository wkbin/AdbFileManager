package view

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import model.Bookmark
import model.FileItem
import model.SortType
import model.StringsManager
import org.koin.compose.viewmodel.koinViewModel
import view.components.CreateDirectoryDialog
import view.components.CreateFileDialog
import view.components.EmptyView
import view.components.ErrorView
import view.components.FileListItem
import view.components.LoadingView
import view.components.ChangePermissionsDialog
import view.components.RenameDialog
import view.components.TransferProgressDialog
import view.components.PathNavigator
import view.components.toast.ToastModel
import view.components.toast.ToastUI
import view.components.toast.ToastUIState
import view.components.ToolbarButton
import view.components.dnd.adbDndSource
import view.components.dnd.adbDndTarget
import view.components.isEditableFile
import view.effect.FileManagerEffect
import view.intent.FileManagerIntent
import viewmodel.FileManagerViewModel
import view.components.SearchDialog
import view.components.AppManagerDialog
import view.components.ClipboardDialog

@Composable
fun FileManagerScreen(viewModel: FileManagerViewModel = koinViewModel<FileManagerViewModel>()) {
    var showSearch by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()
    
    val allFiles = remember(state.files) {
        when (val filesState = state.files) {
            is LoadState.Success -> filesState.data
            else -> emptyList()
        }
    }
    val strings by StringsManager.strings.collectAsState()

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
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PathNavigator(
                    currentPath = viewModel.directoryList,
                    onPathClick = { index ->
                        viewModel.dispatch(FileManagerIntent.NavigateToPathIndex(index))
                    },
                    onPathInput = { path ->
                        viewModel.dispatch(FileManagerIntent.ResetDirectory(path))
                    },
                    modifier = Modifier.weight(1f)
                )

                // 书签按钮放在搜索旁边
                IconButton(
                    onClick = {
                        if (state.currentPathBookmarked) {
                            state.bookmarks.find { it.path == "/${viewModel.directoryList.joinToString("/")}" }?.let { bookmark ->
                                viewModel.dispatch(FileManagerIntent.RemoveBookmark(bookmark))
                            }
                        } else {
                            viewModel.dispatch(FileManagerIntent.RequestAddBookmarkDialog)
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (state.currentPathBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                         contentDescription = if (state.currentPathBookmarked) strings.removeBookmark else strings.addBookmark,
                        tint = if (state.currentPathBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 搜索按钮放在路径导航旁边
                IconButton(
                    onClick = { showSearch = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = strings.searchTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            FileToolBar(
                currentSortType = state.sortType,
                onBackClick = {
                    viewModel.dispatch(FileManagerIntent.NavigateUp)
                }, 
                onSortTypeChange = {
                    viewModel.dispatch(FileManagerIntent.ResetSortType(it))
                },
                onCreateDirectoryClick = {
                    viewModel.dispatch(FileManagerIntent.ShowCreateDirectoryDialog)
                },
                onCreateFileClick = {
                    viewModel.dispatch(FileManagerIntent.ShowCreateFileDialog)
                },
                bookmarks = state.bookmarks,
                currentPathBookmarked = state.currentPathBookmarked,
                showBookmarkMenu = state.showBookmarkMenu,
                onBookmarkMenuClick = {
                    viewModel.dispatch(FileManagerIntent.RequestBookmarkMenu)
                },
                onDismissBookmarkMenu = {
                    viewModel.dispatch(FileManagerIntent.DismissBookmarkMenu)
                },
                onAddBookmarkClick = {
                    viewModel.dispatch(FileManagerIntent.RequestAddBookmarkDialog)
                },
                onRemoveBookmarkClick = { bookmark ->
                    viewModel.dispatch(FileManagerIntent.RemoveBookmark(bookmark))
                },
                onNavigateToBookmarkClick = { bookmark ->
                    viewModel.dispatch(FileManagerIntent.NavigateToBookmark(bookmark))
                },
                onAppsClick = { viewModel.dispatch(FileManagerIntent.ShowAppManager) },
                onScreenshotClick = { viewModel.dispatch(FileManagerIntent.TakeScreenshot) },
                onClipboardClick = { viewModel.dispatch(FileManagerIntent.ShowClipboardDialog) }
            )
            // 操作进行中的进度指示器
            AnimatedVisibility(
                visible = state.operationInProgress
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp)
                )
            }
            FileListView(viewModel)
        }
    }

    state.pendingDeleteFile?.let { fileName ->
        AlertDialog(
            onDismissRequest = {
                viewModel.dispatch(FileManagerIntent.CancelDeleteConfirmation)
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = strings.fileDeleteConfirm,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = strings.fileDeleteMessage(fileName),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dispatch(FileManagerIntent.DeleteFile(fileName))
                    }
                ) {
                    Text(
                        text = strings.fileDelete,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dispatch(FileManagerIntent.CancelDeleteConfirmation)
                    }
                ) {
                    Text(strings.fileCancel)
                }
            },
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    CreateDirectoryDialog(
        visible = state.showCreateDirectoryDialog,
        onDismiss = { 
            viewModel.dispatch(FileManagerIntent.DismissCreateDialogs) 
        },
        onConfirm = { dirName ->
            viewModel.dispatch(FileManagerIntent.CreateDirectory(dirName))
        }
    )

    CreateFileDialog(
        visible = state.showCreateFileDialog,
        onDismiss = { 
            viewModel.dispatch(FileManagerIntent.DismissCreateDialogs) 
        },
        onConfirm = { fileName, content ->
            viewModel.dispatch(FileManagerIntent.CreateFile(fileName, content))
        }
    )

    AddBookmarkDialog(
        visible = state.showAddBookmarkDialog,
        currentPath = "/${viewModel.directoryList.joinToString("/")}",
        onDismiss = { 
            viewModel.dispatch(FileManagerIntent.DismissAddBookmarkDialog) 
        },
        onConfirm = { name ->
            viewModel.dispatch(FileManagerIntent.AddBookmark(name))
        }
    )

    ChangePermissionsDialog(
        visible = state.showChangePermissionsDialog,
        file = state.pendingChangePermissionsFile,
        onDismiss = { 
            viewModel.dispatch(FileManagerIntent.DismissChangePermissionsDialog) 
        },
        onConfirm = { fileName, permissions ->
            viewModel.dispatch(FileManagerIntent.ChangePermissions(fileName, permissions))
        }
    )

    state.pendingRenameFile?.let { file ->
        RenameDialog(
            visible = true,
            file = file,
            onDismiss = {
                viewModel.dispatch(FileManagerIntent.DismissRenameDialog)
            },
            onConfirm = { newName ->
                viewModel.dispatch(FileManagerIntent.RenameFile(file.fileName, newName))
            }
        )
    }

    // 复制/移动对话框
    if (state.showCopyMoveDialog && state.pendingCopyMoveFile != null) {
        var destPath by remember { mutableStateOf("/" + viewModel.directoryList.joinToString("/")) }
        val isCopy = state.isCopyOperation
        val strings by StringsManager.strings.collectAsState()

        AlertDialog(
            onDismissRequest = {
                viewModel.dispatch(FileManagerIntent.DismissCopyMoveDialog)
            },
            title = {
                Text(
                    text = if (isCopy) strings.fileCopyTo else strings.fileMoveTo,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "${strings.fileName}: ${state.pendingCopyMoveFile!!.fileName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = destPath,
                        onValueChange = { destPath = it },
                        label = { Text(strings.destinationPath) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = if (isCopy)
                            FileManagerIntent.CopyFile(state.pendingCopyMoveFile!!.fileName, destPath)
                        else
                            FileManagerIntent.MoveFile(state.pendingCopyMoveFile!!.fileName, destPath)
                        viewModel.dispatch(intent)
                    },
                    enabled = destPath.isNotEmpty()
                ) {
                    Text(if (isCopy) strings.fileCopy else strings.fileMove)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dispatch(FileManagerIntent.DismissCopyMoveDialog) }
                ) {
                    Text(strings.fileCancel)
                }
            }
        )
    }

    TransferProgressDialog(
        visible = state.showTransferDialog,
        fileName = state.transferringFileName,
        transferState = viewModel.transferProgress,
        onDismiss = {
            viewModel.dispatch(FileManagerIntent.EndTransfer)
        },
        onCancel = {
            viewModel.dispatch(FileManagerIntent.EndTransfer)
        }
    )
    
    SearchDialog(
        visible = showSearch,
        onDismiss = { showSearch = false },
        allFiles = allFiles,
        viewModel = viewModel
    )

    AppManagerDialog(
        visible = state.showAppManager,
        appList = state.appList,
        isLoading = state.appListLoading,
        onDismiss = { viewModel.dispatch(FileManagerIntent.DismissAppManager) },
        onUninstall = { viewModel.dispatch(FileManagerIntent.UninstallApp(it)) },
        onBackup = { pkg, dest ->
            viewModel.dispatch(FileManagerIntent.BackupApk(pkg, dest))
        }
    )

    ClipboardDialog(
        visible = state.showClipboardDialog,
        onDismiss = { viewModel.dispatch(FileManagerIntent.DismissClipboardDialog) },
        onPush = { viewModel.dispatch(FileManagerIntent.PushClipboard(it)) }
    )
}

@Composable
private fun AddBookmarkDialog(
    visible: Boolean,
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    if (!visible) return
    val strings by StringsManager.strings.collectAsState()
    var bookmarkName by remember { 
        mutableStateOf(
            if (currentPath == "/") strings.navRoot
            else currentPath.split("/").lastOrNull() ?: strings.bookmark
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.addBookmark,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = strings.bookmarkPath(currentPath),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = bookmarkName,
                    onValueChange = { bookmarkName = it },
                    label = { Text(strings.bookmarkNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(bookmarkName) },
                enabled = bookmarkName.isNotBlank()
            ) {
                Text(strings.bookmarkConfirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.bookmarkCancel)
            }
        }
    )
}

@Composable
private fun FileToolBar(
    currentSortType: SortType,
    onBackClick: () -> Unit,
    onSortTypeChange: (SortType) -> Unit,
    onCreateDirectoryClick: () -> Unit,
    onCreateFileClick: () -> Unit,
    bookmarks: List<Bookmark>,
    currentPathBookmarked: Boolean,
    showBookmarkMenu: Boolean,
    onBookmarkMenuClick: () -> Unit,
    onDismissBookmarkMenu: () -> Unit,
    onAddBookmarkClick: () -> Unit,
    onRemoveBookmarkClick: (Bookmark) -> Unit,
    onNavigateToBookmarkClick: (Bookmark) -> Unit,
    onAppsClick: () -> Unit = {},
    onScreenshotClick: () -> Unit = {},
    onClipboardClick: () -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    val strings by StringsManager.strings.collectAsState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                ToolbarButton(
                    onClick = onBackClick,
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    text = strings.navBack,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))

                Box {
                    ToolbarButton(
                        onClick = { showCreateMenu = true },
                        icon = Icons.Outlined.Add,
                        text = strings.fileCreate,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    DropdownMenu(
                        expanded = showCreateMenu,
                        onDismissRequest = { showCreateMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.fileNewFolder) },
                            onClick = {
                                showCreateMenu = false
                                onCreateDirectoryClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CreateNewFolder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.fileNewFile) },
                            onClick = {
                                showCreateMenu = false
                                onCreateFileClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))

                Box {
                    ToolbarButton(
                        onClick = onBookmarkMenuClick,
                        icon = if (currentPathBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        text = strings.bookmark,
                        tint = if (currentPathBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DropdownMenu(
                        expanded = showBookmarkMenu,
                        onDismissRequest = onDismissBookmarkMenu
                    ) {
                        if (bookmarks.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(strings.errorNoFiles) },
                                onClick = { }
                            )
                        } else {
                            bookmarks.forEach { bookmark ->
                                DropdownMenuItem(
                                    text = { Text(bookmark.name) },
                                    onClick = {
                                        onNavigateToBookmarkClick(bookmark)
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { onRemoveBookmarkClick(bookmark) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = strings.fileDelete,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                )
                            }
                            HorizontalDivider()
                        }
                        
                        if (!currentPathBookmarked) {
                            DropdownMenuItem(
                                text = { Text(strings.bookmarkAddCurrentPath) },
                                onClick = {
                                    onDismissBookmarkMenu()
                                    onAddBookmarkClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.BookmarkBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))

                Box {
                    ToolbarButton(
                        onClick = { showSortMenu = true },
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        text = strings.sortTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortType.entries.forEach { sortType ->
                            DropdownMenuItem(
                                text = { Text(sortType.displayName) },
                                onClick = {
                                    onSortTypeChange(sortType)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (currentSortType == sortType) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

            }

            // Right side: utility buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolbarButton(
                    onClick = onAppsClick,
                    icon = Icons.Outlined.Apps,
                    text = "Apps",
                    tint = MaterialTheme.colorScheme.primary
                )
                ToolbarButton(
                    onClick = onScreenshotClick,
                    icon = Icons.Outlined.CameraAlt,
                    text = "Shot",
                    tint = MaterialTheme.colorScheme.primary
                )
                ToolbarButton(
                    onClick = onClipboardClick,
                    icon = Icons.Outlined.ContentPaste,
                    text = "Clip",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileListView(viewModel: FileManagerViewModel) {
    val state by viewModel.state.collectAsState()
    val toastUiState = remember { ToastUIState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FileManagerEffect.ShowSuccessTips -> {
                    toastUiState.show(ToastModel(effect.message, ToastModel.Type.Success))
                }

                is FileManagerEffect.ShowErrorTips -> {
                    toastUiState.show(ToastModel(effect.message, ToastModel.Type.Error))
                }
            }
        }
    }

    when (state.files) {
        is LoadState.Idle -> {}
        is LoadState.Loading -> {
            LoadingView()
        }

        is LoadState.Empty -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .adbDndTarget { files ->
                        viewModel.dispatch(FileManagerIntent.ImportFiles(files))
                    }
            ) {
                EmptyView()
            }
        }

        is LoadState.Error -> {
            val error = (state.files as LoadState.Error).error
            ErrorView(error.message.toString())
        }

        is LoadState.Success -> {
            val data = (state.files as LoadState.Success<FileItem>).data
            val listState = rememberLazyListState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .adbDndTarget { files ->
                        viewModel.dispatch(FileManagerIntent.ImportFiles(files))
                    }
            ) {
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
                        items = data,
                        key = { it.fileName }
                    ) { file ->
                        FileListItem(
                            file = file,
                            onFileClick = {
                                if (file.isDir) {
                                    if (file.link != null) {
                                        viewModel.dispatch(FileManagerIntent.NavigateTo(file.link))
                                    } else {
                                        viewModel.dispatch(FileManagerIntent.NavigateTo(file.fileName))
                                    }
                                } else {
                                    if (isEditableFile(file.fileName)) {
                                        viewModel.dispatch(FileManagerIntent.LoadFileContent(file.fileName))
                                    }
                                }
                            },
                            onEditFile = {
                                viewModel.dispatch(FileManagerIntent.LoadFileContent(file.fileName))
                            },
                            onDeleteFile = {
                                viewModel.dispatch(FileManagerIntent.RequestDeleteConfirmation(file.fileName))
                            },
                            onDownloadFile = {
                                // Download to system Downloads folder
                                val downloadsDir = java.io.File(System.getProperty("user.home"), "Downloads")
                                if (downloadsDir.exists()) {
                                    viewModel.dispatch(
                                        FileManagerIntent.ExportFile(
                                            file.fileName,
                                            downloadsDir.absolutePath + java.io.File.separator + file.fileName
                                        )
                                    )
                                }
                            },
                            onChangePermissions = {
                                viewModel.dispatch(FileManagerIntent.RequestChangePermissions(file))
                            },
                            onRenameFile = {
                                viewModel.dispatch(FileManagerIntent.RequestRename(file))
                            },
                            onInstallApk = {
                                viewModel.dispatch(FileManagerIntent.InstallApk(file.fileName))
                            },
                            onCopyFile = {
                                viewModel.dispatch(FileManagerIntent.RequestCopy(file))
                            },
                            onMoveFile = {
                                viewModel.dispatch(FileManagerIntent.RequestMove(file))
                            },
                            modifier = Modifier.adbDndSource(
                                selectedFile = "/${viewModel.directoryList.joinToString("/")}/${file.fileName}",
                                onRequestExport = { destinationPath ->
                                    // Synchronous pull for DND — ensures file is fully downloaded
                                    // before the external drop target receives it
                                    viewModel.exportFileSync(file.fileName, destinationPath)
                                }
                            )
                        )
                    }
                }

                Box(
                    Modifier.padding(bottom = 50.dp).fillMaxWidth()
                        .align(alignment = Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
                    ToastUI(hostState = toastUiState)
                }
            }
        }
    }
}
