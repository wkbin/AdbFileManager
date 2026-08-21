import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.skiko.hostOs
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import model.StringsManager
import runtime.AdbStore
import runtime.ContextStore
import top.wkbin.filemanager.generated.resources.Res
import utils.CrashHandler
import view.MainScreen
import view.theme.AdbFileManagerTheme
import java.io.File
import java.awt.Dimension
import java.awt.GraphicsEnvironment

// Composition locals
val LocalWindow = compositionLocalOf<ComposeWindow> { error("Window not provided") }

/**
 * 应用程序入口点
 */
fun main() {
    // 1. 最先初始化全局未捕获异常捕获器，防止无声闪退
    CrashHandler.init()

    try {
        StringsManager.init()

        val adbStore = AdbStore(ContextStore().fileDir)
        val koin = startKoin {
            modules(
                module {
                    single { adbStore }
                },
                adbModule, viewModelModule
            )
            printLogger()
        }.koin

        launchApplication(adbStore, koin)
    } catch (t: Throwable) {
        CrashHandler.logError("应用程序启动阶段发生未捕获致命错误", t)
        throw t
    }
}

private fun launchApplication(adbStore: AdbStore, koin: Koin) = application {

    var isRuntimeInitialized by remember { mutableStateOf(false) }
    var runtimeInitError by remember { mutableStateOf<String?>(null) }

    val availableScreen = remember {
        GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    }
    val initialWindowSize = remember(availableScreen.width, availableScreen.height) {
        calculateInitialWindowSize(availableScreen.width, availableScreen.height)
    }

    // 初始化ADB运行时
    initAdbRuntime(adbStore) { error ->
        runtimeInitError = error
        isRuntimeInitialized = true
    }

    // 主应用程序窗口
    Window(
        title = StringsManager.strings.value.appTitle,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            width = initialWindowSize.width.dp,
            height = initialWindowSize.height.dp
        ),
        onCloseRequest = ::exitApplication,
        undecorated = true  // 移除默认窗口装饰
    ) {
        LaunchedEffect(window, availableScreen.width, availableScreen.height) {
            window.minimumSize = Dimension(
                minOf(900, (availableScreen.width * 0.85).toInt()),
                minOf(600, (availableScreen.height * 0.85).toInt())
            )
        }

        CompositionLocalProvider(
            LocalWindow provides window
        ) {
            AdbFileManagerTheme {
                if (isRuntimeInitialized) {
                    MainScreen(
                        shellSessionFactory = koin.get(),
                        onCloseRequest = ::exitApplication
                    )
                } else {
                    // 启动加载画面（防止未完成解压时窗口透明空白无反应）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "正在准备 ADB 运行环境…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 初始化ADB运行时
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun initAdbRuntime(adbStore: AdbStore, onInitialized: (error: String?) -> Unit) {
    LaunchedEffect(Unit) {
        var errorMsg: String? = null
        try {
            adbStore.installRuntime(
                Res.readBytes(adbStore.resourceName),
                adbStore.adbHostFile.absolutePath
            )

            // 检查并修复 ADB 执行权限（针对 Linux/Mac 系统）
            if (!hostOs.isWindows) {
                val adbExecutable = File(adbStore.adbHostFile, "adb")
                if (adbExecutable.exists() && !adbExecutable.canExecute()) {
                    withContext(Dispatchers.IO) {
                        val success = adbExecutable.setExecutable(true, false)
                        if (!success) {
                            ProcessBuilder("chmod", "755", adbExecutable.absolutePath)
                                .start()
                                .waitFor()
                        }
                        println(StringsManager.strings.value.adbPermissionSet(adbExecutable.absolutePath))
                    }
                }
            }
        } catch (e: Exception) {
            errorMsg = e.message
            CrashHandler.logError("初始化内置 ADB 运行时失败: ${e.message}", e)
        } finally {
            onInitialized(errorMsg)
        }
    }
}
