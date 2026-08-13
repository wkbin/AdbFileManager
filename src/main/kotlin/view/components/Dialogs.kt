package view.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.StringsManager
import java.awt.Desktop
import java.net.URI
import utils.UpdateInfo
import utils.VersionChecker

/**
 * 创建目录对话框
 */
@Composable
fun CreateDirectoryDialog(
    visible: Boolean,
    onDismiss: () -> Unit = {},
    onDismissRequest: () -> Unit = onDismiss,
    onConfirm: (dirName: String) -> Unit
) {
    if (!visible) return
    val strings by StringsManager.strings.collectAsState()
    var dirName by remember { mutableStateOf("") }
    val isInputValid = dirName.isNotEmpty() && !dirName.contains("/") && !dirName.contains("\\")
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // 自动聚焦到输入框
    LaunchedEffect(visible) {
        delay(100) // 短暂延迟以确保UI完全渲染
        focusRequester.requestFocus()
    }
    
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题和图标
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = strings.fileNewFolder,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = strings.editorClose,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 输入框
                OutlinedTextField(
                    value = dirName,
                    onValueChange = { dirName = it },
                    label = { 
                        Text(strings.fileFolderName) 
                    },
                    placeholder = {
                        Text(
                            strings.fileFolderNameInput,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    isError = dirName.isNotEmpty() && !isInputValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onKeyEvent {
                            if (it.key == Key.Enter && isInputValid) {
                                onConfirm(dirName)
                                true
                            } else {
                                false
                            }
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isInputValid) {
                                focusManager.clearFocus()
                                onConfirm(dirName)
                            }
                        }
                    ),
                    supportingText = {
                        if (dirName.isNotEmpty() && !isInputValid) {
                            Text(
                                strings.fileFolderNameErrorInvalid,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Create,
                            contentDescription = null
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(strings.fileCancel)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = { 
                            if (isInputValid) {
                                onConfirm(dirName)
                                dirName = ""
                            }
                        },
                        enabled = isInputValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(strings.fileCreate)
                    }
                }
            }
        }
    }
}

/**
 * 创建文件对话框
 */
@Composable
fun CreateFileDialog(
    visible: Boolean,
    onDismiss: () -> Unit = {},
    onDismissRequest: () -> Unit = onDismiss,
    onConfirm: (fileName: String, content: String) -> Unit
) {
    val strings by StringsManager.strings.collectAsState()
    if (visible) {
        var fileName by remember { mutableStateOf("") }
        var fileContent by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        
        // 验证文件名
        val validateFileName = {
            when {
                fileName.isBlank() -> {
                    isError = true
                    errorMessage = strings.fileNameEmptyError
                    false
                }
                fileName.contains("/") || fileName.contains("\\") -> {
                    isError = true
                    errorMessage = strings.fileNameInvalidError
                    false
                }
                else -> {
                    isError = false
                    errorMessage = ""
                    true
                }
            }
        }
        
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = strings.fileNewFile,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // 文件名输入框
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { 
                            fileName = it
                            if (isError) validateFileName()
                        },
                        label = { Text(strings.fileName) },
                        placeholder = { Text(strings.fileNameInput) },
                        singleLine = true,
                        isError = isError,
                        supportingText = {
                            if (isError) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 文件内容输入框
                    OutlinedTextField(
                        value = fileContent,
                        onValueChange = { fileContent = it },
                        label = { Text(strings.fileContent) },
                        placeholder = { Text(strings.fileContentInput) },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (validateFileName()) {
                            onConfirm(fileName, fileContent)
                        }
                    },
                    enabled = fileName.isNotBlank()
                ) {
                    Text(strings.fileCreate)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(strings.fileCancel)
                }
            }
        )
    }
}

/**
 * 关于对话框
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    // 检查更新状态
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val strings by StringsManager.strings.collectAsState()
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 应用图标
                Icon(
                    imageVector = Icons.Filled.Android,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 应用名称
                Text(
                    text = strings.appTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 版本信息
                Text(
                    text = strings.appVersion(Constants.VERSION),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 描述
                Text(
                    text = strings.aboutDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮区域
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // GitHub 链接
                    Button(
                        onClick = {
                            try {
                                Desktop.getDesktop().browse(URI(Constants.GITHUB_URL))
                            } catch (e: Exception) {
                                // 处理打开链接失败的情况
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.width(200.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.aboutViewOnGithub)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 检查更新按钮
                    Button(
                        onClick = {
                            isCheckingUpdate = true
                            coroutineScope.launch {
                                val update = VersionChecker.checkForUpdates(forceCheck = true)
                                isCheckingUpdate = false
                                if (update != null) {
                                    updateAvailable = update
                                    showUpdateDialog = true
                                } else {
                                    // 显示已是最新版本的提示
                                    // 可以使用 SnackBar 或者其他方式提示
                                }
                            }
                        },
                        enabled = !isCheckingUpdate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.width(200.dp)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.aboutChecking)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Update,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.aboutCheckUpdate)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 关闭按钮
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(strings.aboutClose)
                }
            }
        }
    }

    if (showUpdateDialog){
        // 显示更新对话框
        updateAvailable?.let { info ->
            UpdateDialog(
                updateInfo = info,
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}

/**
 * 更新提示对话框
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    var neverShowUpdates by remember { mutableStateOf(false) }
    val strings by StringsManager.strings.collectAsState()
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 更新图标
                Icon(
                    imageVector = Icons.Filled.Update,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 标题
                Text(
                    text = strings.appNewVersion,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 版本信息
                Text(
                    text = strings.appNewVersionDetail(updateInfo.version),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 更新说明
                Text(
                    text = strings.appUpdateContent,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 更新说明内容
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, false)
                        .heightIn(max = 200.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 永不提示选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = neverShowUpdates,
                        onCheckedChange = { neverShowUpdates = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = strings.appDontPromptAgain,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    // 稍后再说按钮
                    TextButton(
                        onClick = {
                            // 如果选择了永不提示，保存配置
                            if (neverShowUpdates) {
                                VersionChecker.saveUpdateConfig(neverShowUpdates = true)
                            } else {
                                // 否则只忽略当前版本
                                VersionChecker.saveUpdateConfig(ignoredVersion = updateInfo.version)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(strings.appLater)
                    }

                    // 立即更新按钮
                    Button(
                        onClick = {
                            try {
                                Desktop.getDesktop().browse(URI(updateInfo.downloadUrl))
                                // 如果选择了永不提示，保存配置
                                if (neverShowUpdates) {
                                    VersionChecker.saveUpdateConfig(neverShowUpdates = true)
                                }
                            } catch (e: Exception) {
                                // 处理打开链接失败的情况
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.appUpdateNow)
                    }
                }
            }
        }
    }
}
