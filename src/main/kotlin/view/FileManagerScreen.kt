package view

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.launch
import view.components.*
import view.theme.AdbFileManagerTheme
import viewmodel.FileManagerViewModel
import java.io.File

/**
 * 文件管理器主屏幕
 */
@Composable
fun FileManagerScreen(viewModel: FileManagerViewModel) {
    val scope = rememberCoroutineScope()
    val files by viewModel.files.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val error by viewModel.error.collectAsState(initial = null)
    
    // 本地UI状态
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showFileEditDialog by remember { mutableStateOf(false) }
    var showFilePicker by remember { mutableStateOf(false) }
    
    // 列表状态，用于滚动相关功能
    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }
    
    // 监听目录变化，重置滚动位置
    LaunchedEffect(viewModel.directoryPath) {
        listState.animateScrollToItem(0)
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
                                            // 跳转到指定路径
                                            viewModel.navigateToPathIndex(index)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    // 搜索按钮放在路径导航旁边
                                    IconButton(
                                        onClick = { /* 搜索功能，待实现 */ },
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
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                FileManagerToolbar(
                                    onCreateDirectoryClick = { showCreateDirDialog = true },
                                    onCreateFileClick = { showCreateFileDialog = true },
                                    onRefreshClick = { viewModel.reload() },
                                    onBackClick = { viewModel.navigateUp() },
                                    canNavigateUp = viewModel.canNavigateUp(),
                                    onImportClick = { showFilePicker = true }
                                )
                            }
                        }
                    }
                    
                    // 错误消息
                    Box {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = error != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            error?.let {
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
                    
                    // 文件列表
                    Box(modifier = Modifier.fillMaxSize()) {
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
                            // 文件列表
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(
                                    top = 8.dp,
                                    bottom = 80.dp, // 预留底部空间，避免被FAB遮挡
                                    start = 4.dp,
                                    end = 4.dp
                                )
                            ) {
                                items(
                                    items = files,
                                    key = { it.fileName }
                                ) { file ->
                                    var showDirectoryPicker by remember { mutableStateOf(false) }
                                    DirectoryPicker(showDirectoryPicker) { path ->
                                        showDirectoryPicker = false
                                        path?.let {
                                            viewModel.pullFile(file.fileName, it) {
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
                                                            viewModel.loadFileContent(file.fileName) { _ ->
                                                                showFileEditDialog = true
                                                            }
                                                        }
                                                    }
                                                },
                                                onEditFile = {
                                                    viewModel.loadFileContent(file.fileName) { _ ->
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
                                                }
                                            )
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
                                    scope.launch {
                                        listState.animateScrollToItem(0)
                                    }
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
                if (showCreateDirDialog) {
                    CreateDirectoryDialog(
                        visible = showCreateDirDialog,
                        onDismissRequest = { showCreateDirDialog = false },
                        onConfirm = { dirName ->
                            viewModel.createDirectory(
                                dirName
                            ) {
                                // 刷新文件列表并关闭对话框
                                viewModel.reload()
                                showCreateDirDialog = false
                            }
                        }
                    )
                }
                
                // 创建文件对话框
                if (showCreateFileDialog) {
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
                                showCreateFileDialog = false
                            }
                        }
                    )
                }
                
                // 文件编辑对话框
                if (showFileEditDialog) {
                    FileEditDialog(
                        visible = showFileEditDialog,
                        fileName = viewModel.currentFileName.value,
                        initialContent = viewModel.currentFileContent.value,
                        onDismiss = { showFileEditDialog = false },
                        onSave = { content ->
                            viewModel.saveFileContent(content) {
                                showFileEditDialog = false
                            }
                        }
                    )
                }
                
                // 文件选择器
                FilePicker(showFilePicker) { path ->
                    showFilePicker = false
                    path?.let {
                        // 复制文件到当前目录
                        val file = File(it.path)
                        viewModel.importFile(file) {
                            // 刷新文件列表
                            viewModel.reload()
                        }
                    }
                }
            }
        }
    }
} 