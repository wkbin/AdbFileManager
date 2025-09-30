package view.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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

@Composable
fun SearchDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    allFiles: List<FileItem>,
    viewModel: FileManagerViewModel
) {
    if (!visible) return

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val strings by StringsManager.strings.collectAsState()

    val filteredFiles = remember(searchQuery, allFiles) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allFiles.filter { 
                it.fileName.contains(searchQuery, ignoreCase = true) 
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
                            items(filteredFiles) { file ->
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
