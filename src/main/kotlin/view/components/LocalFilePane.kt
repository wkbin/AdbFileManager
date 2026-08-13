package view.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.East
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import model.FileItem
import model.FileUtils
import model.StringsManager
import model.isLikelyWritableLocalDirectory
import view.components.dnd.adbDndTarget
import view.components.dnd.localFileDndSource
import java.io.File

@Composable
fun LocalFilePane(
    isActive: Boolean,
    onActivate: () -> Unit,
    onPathChanged: (File) -> Unit,
    onCopyToRemote: (files: List<File>) -> Unit,
    onDropFromRemote: (files: List<File>, destinationDirectory: File) -> Unit,
    refreshVersion: Int,
    collapsed: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings by StringsManager.strings.collectAsState()
    var directory by remember { mutableStateOf(initialLocalDirectory()) }
    var refreshToken by remember { mutableStateOf(0) }
    var pathText by remember(directory) { mutableStateOf(directory.absolutePath) }
    var showRoots by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember(directory) { mutableStateOf(emptySet<String>()) }

    val localFiles = remember(directory, refreshToken) {
        runCatching {
            directory.listFiles().orEmpty()
                .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
        }.getOrDefault(emptyList())
    }
    val itemsByPath = remember(localFiles) { localFiles.associateBy { it.absolutePath } }
    val directoryWritable = remember(directory, refreshToken) {
        isLikelyWritableLocalDirectory(directory)
    }

    fun navigate(target: File) {
        if (!target.isDirectory) return
        directory = target.absoluteFile
        selected = emptySet()
        onPathChanged(directory)
        onActivate()
    }

    LaunchedEffect(Unit) { onPathChanged(directory) }
    LaunchedEffect(refreshVersion) { refreshToken++ }

    Surface(
        modifier = modifier.border(
            if (isActive) 2.dp else 1.dp,
            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(10.dp)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        if (collapsed) {
            Column(
                modifier = Modifier.fillMaxSize().adbDndTarget { files ->
                    onDropFromRemote(files, directory)
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = { onCollapsedChange(false) }) {
                    Icon(Icons.Outlined.ChevronRight, strings.localPaneExpand)
                }
                Icon(
                    Icons.Outlined.Computer,
                    contentDescription = strings.panelLeft,
                    modifier = Modifier.size(22.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Surface
        }

        Column(
            Modifier.fillMaxSize().adbDndTarget { files ->
                onDropFromRemote(files, directory)
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onActivate),
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.panelLeft, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    if (!directoryWritable) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            strings.localDirectoryReadOnly,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        onClick = { onCollapsedChange(true) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Outlined.ChevronLeft, strings.localPaneCollapse)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { directory.parentFile?.let(::navigate) }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.navBack)
                }
                Box {
                    IconButton(onClick = { showRoots = true }) {
                        Icon(Icons.Outlined.Storage, "磁盘")
                    }
                    DropdownMenu(showRoots, onDismissRequest = { showRoots = false }) {
                        File.listRoots().forEach { root ->
                            DropdownMenuItem(
                                text = { Text(root.absolutePath) },
                                onClick = { showRoots = false; navigate(root) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = pathText,
                    onValueChange = { pathText = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall,
                    trailingIcon = {
                        IconButton(onClick = { navigate(File(pathText)) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "打开路径")
                        }
                    }
                )
                IconButton(onClick = { refreshToken++ }) {
                    Icon(Icons.Outlined.Refresh, "刷新")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(
                    icon = if (selectionMode) Icons.Outlined.Deselect else Icons.Outlined.CheckBox,
                    text = if (selectionMode) "退出多选" else "多选",
                    compact = true,
                    onClick = { selectionMode = !selectionMode; if (!selectionMode) selected = emptySet() }
                )
                if (selectionMode) {
                    Spacer(Modifier.width(4.dp))
                    Text("已选择 ${selected.size} 项", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    ToolbarButton(
                        icon = Icons.Outlined.East,
                        text = strings.localCopyToAndroid,
                        compact = true,
                        enabled = selected.isNotEmpty(),
                        onClick = { onCopyToRemote(selected.mapNotNull(itemsByPath::get)) }
                    )
                }
            }

            if (localFiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.errorEmptyDirectory, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    items(localFiles, key = { it.absolutePath }) { file ->
                        val item: FileItem = FileUtils.fromLocalFile(file)
                        FileListItem(
                            file = item,
                            compact = true,
                            allowEdit = false,
                            allowAdministrativeActions = false,
                            allowMove = false,
                            showDragHandle = true,
                            dragHandleModifier = Modifier.localFileDndSource(file),
                            copyIcon = Icons.Outlined.East,
                            selectionMode = selectionMode,
                            isSelected = file.absolutePath in selected,
                            onSelectionChange = {
                                selected = if (file.absolutePath in selected) selected - file.absolutePath else selected + file.absolutePath
                            },
                            onFileClick = { if (file.isDirectory) navigate(file) },
                            onEditFile = {},
                            onDeleteFile = {},
                            onDownloadFile = {},
                            onChangePermissions = {},
                            onRenameFile = {},
                            onCopyFile = { onCopyToRemote(listOf(file)) },
                            onMoveFile = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun initialLocalDirectory(): File =
    File(System.getProperty("user.home")).takeIf { it.isDirectory }
        ?: File.listRoots().firstOrNull()
        ?: File(".").absoluteFile
