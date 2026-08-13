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
}

private fun launchApplication(adbStore: AdbStore, koin: Koin) = application {

    var isRuntimeInitialized by remember { mutableStateOf(false) }
    val availableScreen = remember {
        GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    }
    val initialWindowSize = remember(availableScreen.width, availableScreen.height) {
        calculateInitialWindowSize(availableScreen.width, availableScreen.height)
    }

    // 初始化ADB运行时
    initAdbRuntime(adbStore) {
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
        if (isRuntimeInitialized) {
            CompositionLocalProvider(
                LocalWindow provides window
            ) {
                AdbFileManagerTheme {
                    MainScreen(
                        shellSessionFactory = koin.get(),
                        onCloseRequest = ::exitApplication
                    )
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
            println(StringsManager.strings.value.adbPermissionError(e.message ?: ""))
            e.printStackTrace()
        }

        onInitialized()
    }
}

