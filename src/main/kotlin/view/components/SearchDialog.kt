package view.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import model.FileItem
import model.StringsManager
import view.intent.FileManagerIntent
import viewmodel.FileManagerViewModel

private enum class SearchFilter { All, DirectoriesOnly, FilesOnly }

@Composable
fun SearchDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    allFiles: List<FileItem>,
    viewModel: FileManagerViewModel
) {
    if (!visible) return

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(SearchFilter.All) }
    val focusRequester = remember { FocusRequester() }
    val strings by StringsManager.strings.collectAsState()

    val filteredFiles = remember(searchQuery, allFiles, filterType) {
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

    LaunchedEffect(visible) {
        if (visible) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.searchTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.appClose)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 搜索过滤选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                        SearchFilter.All -> "All"
                                        SearchFilter.DirectoriesOnly -> strings.fileDirectory
                                        SearchFilter.FilesOnly -> strings.fileName
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                    if (filteredFiles.isNotEmpty()) {
                        Text(
                            text = "${filteredFiles.size} results",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    searchQuery.isBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.searchPlaceholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    filteredFiles.isEmpty() -> {
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
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(filteredFiles, key = { it.fileName }) { file ->
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
