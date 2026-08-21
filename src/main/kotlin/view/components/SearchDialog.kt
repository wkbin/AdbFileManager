package view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import model.FileItem
import model.StringsManager
import view.intent.FileManagerIntent
import viewmodel.FileManagerViewModel

private enum class SearchScope { CurrentDir, Recursive }
private enum class SearchFilter { All, DirectoriesOnly, FilesOnly }

@Composable
fun SearchDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    allFiles: List<FileItem>,
    viewModel: FileManagerViewModel
) {
    if (!visible) return

    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf(SearchScope.CurrentDir) }
    var filterType by remember { mutableStateOf(SearchFilter.All) }
    val focusRequester = remember { FocusRequester() }
    val strings by StringsManager.strings.collectAsState()

    // Local files filtered in current directory
    val localFilteredFiles = remember(searchQuery, allFiles, filterType) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allFiles.filter { file ->
                val matchesQuery = file.fileName.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (filterType) {
                    SearchFilter.All -> true
                    SearchFilter.DirectoriesOnly -> file.isDir
                    SearchFilter.FilesOnly -> !file.isDir
                }
                matchesQuery && matchesFilter
            }
        }
    }

    // Recursive search results filtered
    val recursiveFilteredFiles = remember(state.recursiveSearchResults, filterType) {
        state.recursiveSearchResults.filter { file ->
            when (filterType) {
                SearchFilter.All -> true
                SearchFilter.DirectoriesOnly -> file.isDir
                SearchFilter.FilesOnly -> !file.isDir
            }
        }
    }

    // Trigger recursive search with debounce
    LaunchedEffect(searchQuery, searchScope) {
        if (searchScope == SearchScope.Recursive && searchQuery.isNotBlank()) {
            delay(400)
            viewModel.dispatch(FileManagerIntent.SearchRecursive(searchQuery))
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.searchTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.appClose)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            if (searchScope == SearchScope.Recursive)
                                "在当前目录及子目录中全局搜索..."
                            else
                                strings.searchPlaceholder
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search Scope & Filter Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Scope toggle
                    FilterChip(
                        selected = searchScope == SearchScope.CurrentDir,
                        onClick = { searchScope = SearchScope.CurrentDir },
                        label = { Text("当前目录") }
                    )
                    FilterChip(
                        selected = searchScope == SearchScope.Recursive,
                        onClick = {
                            searchScope = SearchScope.Recursive
                            if (searchQuery.isNotBlank()) {
                                viewModel.dispatch(FileManagerIntent.SearchRecursive(searchQuery))
                            }
                        },
                        label = { Text("递归深搜 🔍") }
                    )

                    Spacer(Modifier.width(8.dp))
                    VerticalDivider(modifier = Modifier.height(24.dp))
                    Spacer(Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )

                    SearchFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = filterType == filter,
                            onClick = { filterType = filter },
                            label = {
                                Text(
                                    text = when (filter) {
                                        SearchFilter.All -> "全部"
                                        SearchFilter.DirectoriesOnly -> strings.fileDirectory
                                        SearchFilter.FilesOnly -> strings.fileName
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    val resultCount = if (searchScope == SearchScope.Recursive)
                        recursiveFilteredFiles.size
                    else
                        localFilteredFiles.size

                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "$resultCount 个结果",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Results view
                when {
                    searchScope == SearchScope.Recursive && state.isRecursiveSearching -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text("正在递归搜索 Android 存储...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    searchQuery.isBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "输入关键词开始搜索",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    searchScope == SearchScope.Recursive -> {
                        if (recursiveFilteredFiles.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "未找到匹配的文件或目录",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(recursiveFilteredFiles, key = { it.fileName }) { file ->
                                    val fileNameOnly = file.fileName.substringAfterLast('/')
                                    val parentPath = if (file.fileName.contains('/'))
                                        file.fileName.substringBeforeLast('/')
                                    else
                                        ""

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.dispatch(FileManagerIntent.NavigateToFileLocation(file.fileName))
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (file.isDir) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                                                contentDescription = null,
                                                tint = if (file.isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = fileNameOnly,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (parentPath.isNotEmpty()) {
                                                    Text(
                                                        text = parentPath,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (file.isDir) "" else file.size,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        if (localFilteredFiles.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.errorNoFiles,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(localFilteredFiles, key = { it.fileName }) { file ->
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
                                                if (isImageFile(file.fileName)) {
                                                    viewModel.dispatch(FileManagerIntent.PreviewImage(file.fileName))
                                                } else if (isEditableFile(file.fileName)) {
                                                    viewModel.dispatch(FileManagerIntent.LoadFileContent(file.fileName))
                                                }
                                            }
                                            onDismiss()
                                        },
                                        onPreviewImage = {
                                            viewModel.dispatch(FileManagerIntent.PreviewImage(file.fileName))
                                            onDismiss()
                                        },
                                        onEditFile = {
                                            viewModel.dispatch(FileManagerIntent.LoadFileContent(file.fileName))
                                            onDismiss()
                                        },
                                        onDeleteFile = {
                                            viewModel.dispatch(FileManagerIntent.RequestDeleteConfirmation(file.fileName))
                                            onDismiss()
                                        },
                                        onDownloadFile = {
                                            val downloadsDir = java.io.File(System.getProperty("user.home"), "Downloads")
                                            downloadsDir.mkdirs()
                                            viewModel.dispatch(
                                                FileManagerIntent.ExportFile(
                                                    file.fileName,
                                                    java.io.File(downloadsDir, file.fileName).absolutePath
                                                )
                                            )
                                            onDismiss()
                                        },
                                        onChangePermissions = {
                                            viewModel.dispatch(FileManagerIntent.RequestChangePermissions(file))
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
