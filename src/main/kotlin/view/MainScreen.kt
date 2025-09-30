package view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.FrameWindowScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import model.StringsManager
import navigation.Screen
import org.koin.compose.viewmodel.koinViewModel
import view.components.CustomWindowFrame
import view.components.DeviceConnectionWizard
import view.components.UpdateDialog
import viewmodel.MainViewModel

@Composable
fun FrameWindowScope.MainScreen(
    viewModel: MainViewModel = koinViewModel<MainViewModel>(),
    onCloseRequest: () -> Unit
) {
    val navController = rememberNavController()
    val devices by viewModel.devices.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

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
        onCloseRequest = onCloseRequest
    ) {
        NavHost(navController = navController, startDestination = Screen.ConnectionWizard.name) {
            composable(Screen.ConnectionWizard.name) {
                DeviceConnectionWizard(
                    onRefresh = {
                        // Refresh logic
                    },
                    onPairAndConnectDevice = { ipAddress, port, pairingPort, pairingCode ->
                        viewModel.pairAndConnectDevice(ipAddress, port, pairingPort, pairingCode)
                    }
                )
            }
            composable(Screen.FileManager.name) {
                FileManagerScreen()
            }
        }
    }

    if (showUpdateDialog) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { showUpdateDialog = false }
        )
    }
}