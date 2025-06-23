import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.skiko.hostOs
import runtime.AdbStore
import runtime.ContextStore
import runtime.adb.Adb
import runtime.adb.AdbDevicePoller
import runtime.adb.Terminal
import top.wkbin.filemanager.generated.resources.Res
import utils.UpdateInfo
import utils.VersionChecker
import view.FileManagerScreen
import view.components.CustomWindowFrame
import view.components.DeviceConnectionWizard
import view.components.UpdateDialog
import view.theme.AdbFileManagerTheme
import viewmodel.DeviceViewModel
import viewmodel.FileManagerViewModel
import java.io.File

// Composition locals
val LocalWindow = compositionLocalOf<ComposeWindow> { error("Window not provided") }
val LocalAdb = compositionLocalOf<Adb> { error("Adb not provided") }
val LocalAdbStore = compositionLocalOf<AdbStore> { error("AdbStore not provided") }

/**
 * 应用程序入口点
 */
fun main() = application {
    val adbStore = AdbStore(ContextStore().fileDir)
    var isRuntimeInitialized by remember { mutableStateOf(false) }

    // 初始化ADB运行时
    initAdbRuntime(adbStore) {
        isRuntimeInitialized = true
    }

    // 主应用程序窗口
    Window(
        title = "ADB 文件管理器",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
        onCloseRequest = ::exitApplication,
        undecorated = true  // 移除默认窗口装饰
    ) {
        if (isRuntimeInitialized) {
            val adbFile =
                File(adbStore.adbHostFile.absolutePath, if (hostOs.isWindows) "adb.exe" else "adb")
            // 设置依赖项
            val adb = Adb(adbFile.absolutePath, Terminal())
            println("adbPath: ${adb.adbPath}")

            // 提供Composition locals
            CompositionLocalProvider(
                LocalWindow provides window,
                LocalAdb provides adb,
                LocalAdbStore provides adbStore
            ) {
                AdbFileManagerTheme {
                    // 自定义窗口布局，包含自定义标题栏
                    CustomWindowFrame(
                        title = "ADB 文件管理器",
                        onCloseRequest = ::exitApplication
                    ) {
                        AppContent()
                    }
                }
            }
        }
    }
}

/**
 * 主应用程序内容
 */
@Composable
private fun AppContent() {
    val adb = LocalAdb.current
    val coroutineScope = rememberCoroutineScope()

    // 创建设备轮询器
    val adbDevicePoller = remember { AdbDevicePoller(adb, coroutineScope) }

    // 创建视图模型
    val deviceViewModel = remember { DeviceViewModel(adbDevicePoller, coroutineScope) }
    val fileManagerViewModel = remember { FileManagerViewModel(adbDevicePoller, coroutineScope) }

    // 已连接设备状态
    val connectedDevices by deviceViewModel.connectedDevices.collectAsState(initial = emptyList())

    // 更新检查状态
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    // 检查更新
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val newUpdate = VersionChecker.checkForUpdates()
                if (newUpdate != null) {
                    updateInfo = newUpdate
                    showUpdateDialog = true
                }
            } catch (e: Exception) {
                println("检查更新失败: ${e.message}")
            }
        }
    }

    // 根据设备连接状态显示适当的屏幕
    if (connectedDevices.isEmpty()) {
        DeviceConnectionWizard(deviceViewModel) {
            deviceViewModel.loadVirtualDevices()
        }
    } else {
        FileManagerScreen(deviceViewModel, fileManagerViewModel)
    }

    // 显示更新对话框
    updateInfo?.let { info ->
        UpdateDialog(
            visible = showUpdateDialog,
            updateInfo = info,
            onDismiss = { showUpdateDialog = false }
        )
    }
}

/**
 * 初始化ADB运行时
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun initAdbRuntime(adbStore: AdbStore, onInitialized: () -> Unit) {
    LaunchedEffect(Unit) {
        adbStore.installRuntime(
            Res.readBytes(adbStore.resourceName),
            adbStore.adbHostFile.absolutePath
        )

        // 检查并修复 ADB 执行权限（针对 Linux/Mac 系统）
        try {
            if (!hostOs.isWindows) {
                val adbExecutable = File(adbStore.adbHostFile, "adb")
                if (adbExecutable.exists() && !adbExecutable.canExecute()) {
                    // 尝试设置可执行权限
                    withContext(Dispatchers.IO) {
                        val result = Runtime.getRuntime().exec(
                            arrayOf("chmod", "755", adbExecutable.absolutePath)
                        ).waitFor()

                        if (result != 0) {
                            // 如果 chmod 命令失败，尝试使用 ProcessBuilder
                            val processBuilder =
                                ProcessBuilder("chmod", "755", adbExecutable.absolutePath)
                            processBuilder.start().waitFor()
                        }

                        println("已自动设置 ADB 可执行权限: ${adbExecutable.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            println("设置 ADB 可执行权限时出错: ${e.message}")
            e.printStackTrace()
        }

        onInitialized()
    }
}

