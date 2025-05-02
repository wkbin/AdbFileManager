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
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import runtime.AdbStore
import runtime.ContextStore
import runtime.adb.Adb
import runtime.adb.AdbDevicePoller
import runtime.adb.Terminal
import top.wkbin.filemanager.generated.resources.Res
import view.FileManagerScreen
import view.components.CustomWindowFrame
import view.components.DeviceConnectionWizard
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
            // 设置依赖项
            val adb = Adb("${adbStore.adbHostFile.absolutePath}${File.separator}platform-tools${File.separator}adb", Terminal())
            
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
    
    // 根据设备连接状态显示适当的屏幕
    if (connectedDevices.isEmpty()) {
        DeviceConnectionWizard(deviceViewModel){
            deviceViewModel.loadVirtualDevices()
        }
    } else {
        FileManagerScreen(deviceViewModel, fileManagerViewModel)
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
            val osName = System.getProperty("os.name").lowercase()
            if (osName.contains("linux") || osName.contains("mac") || osName.contains("unix")) {
                val adbExecutable = File(adbStore.adbHostFile, "platform-tools/adb")
                if (adbExecutable.exists() && !adbExecutable.canExecute()) {
                    // 尝试设置可执行权限
                    withContext(Dispatchers.IO) {
                        val result = Runtime.getRuntime().exec(
                            arrayOf("chmod", "755", adbExecutable.absolutePath)
                        ).waitFor()
                        
                        if (result != 0) {
                            // 如果 chmod 命令失败，尝试使用 ProcessBuilder
                            val processBuilder = ProcessBuilder("chmod", "755", adbExecutable.absolutePath)
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

