import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource
import runtime.AdbStore
import runtime.ContextStore
import runtime.adb.Adb
import runtime.adb.Terminal
import ui.MainUI
import java.io.File


val LocalWindowMain = compositionLocalOf<ComposeWindow> { error("Not provided.") }
val LocalAdb = compositionLocalOf<Adb> { error("Not provided") }
val LocalAdbRuntime = compositionLocalOf<AdbStore> { error("Not provided.") }

fun main() = application {
    val adbStore = AdbStore(ContextStore().fileDir)
    var isFinish by remember { mutableStateOf(false) }
    initProperties(adbStore) {
        isFinish = true
    }
    Window(
        title = "AdbFileManager",
        state = rememberWindowState(width = 1000.dp, height = 800.dp),
        onCloseRequest = ::exitApplication) {
        if (isFinish) {
            val adb = Adb("${adbStore.adbHostFile.absolutePath}${File.separator}adb", Terminal())
            CompositionLocalProvider(
                LocalWindowMain provides window,
                LocalAdb provides adb,
                LocalAdbRuntime provides adbStore
            ) {
                MainUI()
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun initProperties(adbStore: AdbStore, loadFinishCallback: () -> Unit) {
    LaunchedEffect(Unit) {
        adbStore.installRuntime(
            resource(adbStore.resourceName).readBytes(),
            adbStore.adbHostFile.absolutePath
        )
        loadFinishCallback.invoke()
    }
}

