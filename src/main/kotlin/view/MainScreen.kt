package view

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Add
import io.github.composefluent.icons.regular.Code
import io.github.composefluent.icons.regular.Dismiss
import io.github.composefluent.icons.regular.Folder

import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text
import io.github.composefluent.component.Icon
import view.fluent.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import model.StringsManager
import navigation.Screen
import org.koin.compose.viewmodel.koinViewModel
import view.components.CustomWindowFrame
import view.components.DeviceConnectionWizard
import view.components.SettingsDialog
import viewmodel.MainViewModel
import data.remote.adb.AdbShellSession
import data.remote.adb.AdbShellSessionFactory

@Composable
fun FrameWindowScope.MainScreen(
    viewModel: MainViewModel = koinViewModel<MainViewModel>(),
    shellSessionFactory: AdbShellSessionFactory,
    onCloseRequest: () -> Unit
) {
    val navController = rememberNavController()
    val devices by viewModel.devices.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val adbStore = org.koin.compose.koinInject<runtime.AdbStore>()
    val bundledAdbPath = remember(adbStore) {
        java.io.File(adbStore.adbHostFile, if (org.jetbrains.skiko.hostOs.isWindows) "adb.exe" else "adb").absolutePath
    }
    val terminalSessions = remember { mutableStateListOf<AdbShellSession>() }
    var selectedTerminalIndex by remember { mutableIntStateOf(-1) }

    fun openTerminal() {
        val device = currentDevice ?: return
        terminalSessions += shellSessionFactory.create(device)
        selectedTerminalIndex = terminalSessions.lastIndex
    }

    fun closeTerminal(index: Int) {
        val session = terminalSessions.getOrNull(index) ?: return
        session.close()
        terminalSessions.removeAt(index)
        selectedTerminalIndex = when {
            terminalSessions.isEmpty() -> -1
            index > terminalSessions.lastIndex -> terminalSessions.lastIndex
            else -> index
        }
    }

    DisposableEffect(Unit) {
        onDispose { terminalSessions.forEach(AdbShellSession::close) }
    }

    LaunchedEffect(currentDevice) {
        if (currentDevice == null) {
            navController.navigate(Screen.ConnectionWizard.name) {
                popUpTo(0)
            }
        } else {
            navController.navigate(Screen.FileManager.name) {
                popUpTo(0)
            }
        }
    }

    LaunchedEffect(updateInfo) {
        showUpdateDialog = updateInfo != null
    }

    val strings by StringsManager.strings.collectAsState()

    CustomWindowFrame(
        title = strings.appTitle,
        currentDevice = currentDevice,
        devices = devices,
        onConnect = {
            viewModel.connect(it)
        },
        onTerminalClick = ::openTerminal,
        onSettingsClick = { showSettingsDialog = true },
        onCloseRequest = onCloseRequest
    ) {
        Column(Modifier.fillMaxSize()) {
            if (currentDevice != null) {
                TerminalTabBar(
                    sessions = terminalSessions,
                    selectedIndex = selectedTerminalIndex,
                    onSelectFiles = { selectedTerminalIndex = -1 },
                    onSelectTerminal = { selectedTerminalIndex = it },
                    onCloseTerminal = ::closeTerminal,
                    onNewTerminal = ::openTerminal
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val selectedSession = terminalSessions.getOrNull(selectedTerminalIndex)
                if (selectedSession != null) {
                    TerminalPanel(selectedSession)
                } else {
                    NavHost(navController = navController, startDestination = Screen.ConnectionWizard.name) {
                        composable(Screen.ConnectionWizard.name) {
                            DeviceConnectionWizard(
                                onRefresh = { },
                                onPairAndConnectDevice = { ipAddress, port, pairingPort, pairingCode ->
                                    viewModel.pairAndConnectDevice(ipAddress, port, pairingPort, pairingCode)
                                }
                            )
                        }
                        composable(Screen.FileManager.name) {
                            FileManagerScreen(onOpenSettings = { showSettingsDialog = true })
                        }
                    }
                }
            }
        }
    }

    connectionError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissConnectionError,
            title = { Text(strings.deviceConnectionFailed) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissConnectionError) { Text(strings.appClose) }
            }
        )
    }

    if (showUpdateDialog) {
        view.components.UpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { showUpdateDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            visible = showSettingsDialog,
            bundledAdbPath = bundledAdbPath,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun TerminalTabBar(
    sessions: List<AdbShellSession>,
    selectedIndex: Int,
    onSelectFiles: () -> Unit,
    onSelectTerminal: (Int) -> Unit,
    onCloseTerminal: (Int) -> Unit,
    onNewTerminal: () -> Unit
) {
    val strings by StringsManager.strings.collectAsState()
    Surface(color = FluentTheme.colors.subtleFill.secondary) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .background(
                        if (selectedIndex == -1) FluentTheme.colors.background.solid.tertiary else Color.Transparent,
                        FluentTheme.shapes.control
                    )
                    .clickable(onClick = onSelectFiles)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Regular.Folder, contentDescription = null)
                Text(strings.terminalFilesTab, modifier = Modifier.padding(start = 6.dp))
            }

            sessions.forEachIndexed { index, _ ->
                Row(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .background(
                            if (selectedIndex == index) FluentTheme.colors.background.solid.tertiary else Color.Transparent,
                            FluentTheme.shapes.control
                        )
                        .clickable { onSelectTerminal(index) }
                        .padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Regular.Code, contentDescription = null)
                    Text("${strings.terminalTitle} ${index + 1}", modifier = Modifier.padding(start = 6.dp))
                    IconButton(onClick = { onCloseTerminal(index) }) {
                        Icon(Icons.Regular.Dismiss, contentDescription = strings.terminalClose)
                    }
                }
            }

            IconButton(onClick = onNewTerminal) {
                Icon(Icons.Regular.Add, contentDescription = strings.terminalNew)
            }
        }
    }
}
