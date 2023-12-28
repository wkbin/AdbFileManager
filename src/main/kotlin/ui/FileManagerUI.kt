package ui

import LocalWindowMain
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import runtime.adb.AdbDevicePoller
import utils.FileManagerUtils
import utils.RemountFile
import weight.DropBoxPanel
import java.lang.StringBuilder


@Composable
fun FileManagerUI(adbDevicePoller: AdbDevicePoller) {
    val scope = rememberCoroutineScope()
    val dirList = remember { mutableStateListOf<String>() }
    var fileList by remember { mutableStateOf<List<RemountFile>>(emptyList()) }
    var currentFileName by remember { mutableStateOf("") }

    // 用于手动刷新
    var manualRefresh by remember { mutableStateOf(false) }


    var editString by remember { mutableStateOf("") }
    var editDialog by remember { mutableStateOf(false) }
    FileEditUI(adbDevicePoller, scope, dirList, currentFileName, editDialog, editString) {
        editDialog = false
    }

    var createFileDirDialog by remember { mutableStateOf(false) }
    FileDirCreateUI(adbDevicePoller, scope, dirList, createFileDirDialog) { reuslt ->
        createFileDirDialog = false
        if (reuslt) {
            manualRefresh = !manualRefresh
        }
    }

    FileManagerContentUI(adbDevicePoller, scope, dirList, fileList, manualRefreshCallback = {
        manualRefresh = !manualRefresh
    }, createFileDirCallback = {
        createFileDirDialog = true
    }, currentFileNameCallBack = {
        currentFileName = it
    }, editStringCallback = {
        editString = it
        editDialog = true
    })

    

    LaunchedEffect(dirList.size, manualRefresh) {
        val dir = dirList.joinToString("/")
        adbDevicePoller.exec("shell ls -l -p /${dir} | sort") {
            fileList = if (
                it.firstOrNull()?.startsWith("ls") == true ||
                it.lastOrNull()?.contains("Permission") == true ||
                it.lastOrNull()?.contains("directory") == true
            ) {
                emptyList()
            } else {
                FileManagerUtils.parseLsToRemountFileList(it)
            }
        }
    }

}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileEditUI(
    adbDevicePoller: AdbDevicePoller,
    scope: CoroutineScope,
    dirList: List<String>,
    currentFileName: String,
    editDialog: Boolean,
    editString: String,
    editCommandCallback: () -> Unit
) {
    var edit by remember { mutableStateOf("") }
    edit = editString
    if (editDialog) {
        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        val scrollState = rememberScrollState()
        Window(state = rememberWindowState(width = 600.dp, height = 800.dp),
            title = "编辑", onCloseRequest = {
                editCommandCallback.invoke()
            }) {
            Box {
                BasicTextField(value = edit, onValueChange = {
                    edit = it
                }, modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            scope.launch {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    }

                )
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    adapter = rememberScrollbarAdapter(scrollState)
                )
                Button(
                    modifier = Modifier.padding(bottom = 20.dp, end = 30.dp)
                        .wrapContentSize()
                        .align(Alignment.BottomEnd),
                    onClick = {
                        val escapedJsonString = edit.replace("\"", "\\\"")
                        scope.launch {
                            val dir = dirList.joinToString("/")
                            adbDevicePoller.exec(
                                """shell "cat > /${dir}/${currentFileName}" <<< '$escapedJsonString'"""
                            ) {
                                editCommandCallback.invoke()
                            }
                        }
                    }) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun FileDirCreateUI(
    adbDevicePoller: AdbDevicePoller,
    scope: CoroutineScope,
    dirList: List<String>,
    createFileDirDialog: Boolean,
    createCallback: (isCommand: Boolean) -> Unit
) {

    DialogWindow(
        title = "创建文件夹",
        onCloseRequest = { createCallback.invoke(false) },
        visible = createFileDirDialog,
        content = {
            var dirName by remember { mutableStateOf("") }
            Column {
                OutlinedTextField(value = dirName, onValueChange = {
                    dirName = it
                }, label = {
                    Text("文件夹名称")
                })
                Button(onClick = {
                    if (dirName.isNotEmpty()) {
                        scope.launch {
                            val dir = dirList.joinToString("/")
                            adbDevicePoller.exec("shell mkdir /$dir/$dirName") {
                                dirName = ""
                                createCallback.invoke(true)
                            }
                        }
                    }
                }) {
                    Text("创建")
                }
            }
        })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileManagerContentUI(
    adbDevicePoller: AdbDevicePoller,
    scope: CoroutineScope,
    dirList: SnapshotStateList<String>,
    fileList: List<RemountFile>,
    manualRefreshCallback: () -> Unit,
    createFileDirCallback: () -> Unit,
    currentFileNameCallBack: (value: String) -> Unit,
    editStringCallback: (value: String) -> Unit
) {
    println("list = ${fileList}")
    val window = LocalWindowMain.current
    Box {
        DropBoxPanel(
            modifier = Modifier.fillMaxSize(),
            window = window
        ) {
            if (it.isNotEmpty()) {
                val dir = dirList.joinToString("/")
                it.forEach { path ->
                    adbDevicePoller.exec("push $path /$dir") {
                        manualRefreshCallback.invoke()
                    }
                }
            }
        }
        LazyColumn(modifier = Modifier.padding(5.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            stickyHeader {
                Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(10.dp)) {
                    Text("/${dirList.joinToString("/")}", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    MenuUI(dirList, manualRefreshCallback, createFileDirCallback)
                }
                Divider(modifier = Modifier.fillMaxWidth())
            }

            items(fileList.size, key = { it }) { index ->
                val remountFile = fileList[index]

                var showGetFile by remember { mutableStateOf(false) }
                DirectoryPicker(showGetFile) { path ->
                    showGetFile = false
                    path?.let {
                        val dir = dirList.joinToString("/")
                        adbDevicePoller.exec("pull /${dir}/${remountFile.fileName} $path")
                    }
                }

                FileItem(modifier = Modifier.animateContentSize(), remountFile, object : Action {
                    override fun click() {
                        currentFileNameCallBack.invoke(remountFile.fileName)
                        dirList.add(remountFile.link ?: remountFile.fileName)
                    }

                    override fun delete() {
                        scope.launch {
                            val dir = dirList.joinToString("/")
                            currentFileNameCallBack.invoke(remountFile.fileName)
                            adbDevicePoller.exec("shell rm -rf /${dir}/${remountFile.fileName}") {
                                manualRefreshCallback.invoke()
                            }
                        }
                    }

                    override fun edit() {
                        scope.launch {
                            val dir = dirList.joinToString("/")
                            currentFileNameCallBack.invoke(remountFile.fileName)
                            adbDevicePoller.exec("shell cat /${dir}/${remountFile.fileName}") {
                                val sb = StringBuilder()
                                it.forEach { line ->
                                    sb.append(line)
                                }
                                if (remountFile.fileName.endsWith(".json")) {
                                    editStringCallback.invoke(formatJson(sb.toString()))
                                } else {
                                    editStringCallback.invoke(sb.toString())
                                }
                            }
                        }
                    }

                    override fun pickUp() {
                        currentFileNameCallBack.invoke(remountFile.fileName)
                        showGetFile = true
                    }
                })
            }
        }
    }
}

@Composable
private fun MenuUI(
    dirList: SnapshotStateList<String>,
    manualRefreshCallback: () -> Unit,
    createFileDirCallback: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        Row(modifier = Modifier.clickable {
            if (dirList.isNotEmpty()) {
                dirList.removeAt(dirList.size - 1)
            }
        }, verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier.size(26.dp),
                painter = painterResource(R.icons.iconBack),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text("返回", fontSize = 16.sp)
        }
        Row(modifier = Modifier.clickable {
            createFileDirCallback.invoke()
        }, verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier.size(26.dp),
                painter = painterResource(R.icons.folder),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text("创建", fontSize = 16.sp)
        }
        Row(modifier = Modifier.clickable {
            manualRefreshCallback.invoke()
        }, verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier.size(26.dp),
                painter = painterResource(R.icons.iconRefresh),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text("刷新", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.weight(1f))

    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileItem(modifier: Modifier, remountFile: RemountFile, action: Action) {
    val fileName = remountFile.link?.run { "${remountFile.fileName} -> $this" } ?: remountFile.fileName
    var isDropdownExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()
        .background(if (isDropdownExpanded) Color(0x880000AA) else Color.Transparent)
        .onPointerEvent(PointerEventType.Press) {
            when {
                it.buttons.isPrimaryPressed -> if (remountFile.isDir) {
                    action.click()
                }

                it.buttons.isSecondaryPressed -> {
                    isDropdownExpanded = true
                }
            }
        }) {
        Image(
            modifier = Modifier.size(26.dp),
            painter = painterResource(remountFile.icon),
            contentDescription = null
        )
        Spacer(Modifier.width(5.dp))

        Text(
            color = if (isDropdownExpanded) Color.White else Color.Black,
            modifier = Modifier.width(300.dp),
            text = fileName,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(10.dp))
        Text(
            color = if (isDropdownExpanded) Color.White else Color.Black,
            modifier = Modifier.width(200.dp),
            text = remountFile.date,
            fontSize = 16.sp
        )
        Spacer(Modifier.width(10.dp))
        Box {
            Text(
                color = if (isDropdownExpanded) Color.White else Color.Black,
                modifier = Modifier.width(100.dp),
                text = remountFile.size,
                fontSize = 16.sp
            )
            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = {
                    isDropdownExpanded = false
                }
            ) {
                if (!remountFile.isDir) {
                    DropdownMenuItem(
                        content = {
                            Text("编辑")
                        },
                        onClick = {
                            action.edit()
                            isDropdownExpanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    content = {
                        Text("提取")
                    },
                    onClick = {
                        action.pickUp()
                        isDropdownExpanded = false
                    }
                )
                DropdownMenuItem(
                    content = {
                        Text("删除")
                    },
                    onClick = {
                        action.delete()
                        isDropdownExpanded = false
                    }
                )
            }
        }
    }
}

private val json = Json { prettyPrint = true }
private fun formatJson(jsonString: String): String {
    return try {
        val parsedJson = Json.parseToJsonElement(jsonString)
        json.encodeToString(parsedJson)
    } catch (e: Exception) {
        "Invalid JSON: ${e.message}"
    }
}

private interface Action {
    fun click()
    fun delete()
    fun edit()
    fun pickUp()
}

