package view.components

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text
import view.fluent.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.FileTransferState
import model.StringsManager
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TransferProgressDialog(
    visible: Boolean,
    fileName: String?,
    transferState: StateFlow<FileTransferState>,
    onDismiss: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    if (!visible) return

    val strings by StringsManager.strings.collectAsState()
    val currentState by transferState.collectAsState()

    val progressText = when (val state = currentState) {
        is FileTransferState.Transferring -> {
            if (state.totalBytes > 0) {
                "${(state.progress * 100).toInt()}%"
            } else {
                ""
            }
        }
        else -> ""
    }

    val statusText = when (val state = currentState) {
        is FileTransferState.Transferring -> {
            if (state.totalBytes > 0) {
                strings.transferProgress(state.bytesTransferred, state.totalBytes)
            } else {
                strings.transferInProgress(0)
            }
        }
        is FileTransferState.Completed -> strings.transferComplete
        is FileTransferState.Failed -> state.error
        FileTransferState.Idle -> strings.transferPreparing
    }

    AlertDialog(
        onDismissRequest = { (onCancel ?: onDismiss).invoke() },
        title = {
            Text(
                text = strings.transferFile,
                style = FluentTheme.typography.subtitle
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                fileName?.let {
                    Text(
                        text = it,
                        style = FluentTheme.typography.body,
                        color = FluentTheme.colors.text.text.secondary,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (val state = currentState) {
                        is FileTransferState.Transferring -> {
                            if (state.totalBytes > 0) {
                                // Deterministic progress for push operations
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                )
                            } else {
                                // Indeterminate for pull operations (adb pull has no progress reporting)
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                )
                            }
                        }
                        else -> {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusText,
                            style = FluentTheme.typography.caption,
                            color = FluentTheme.colors.text.text.secondary
                        )
                        if (progressText.isNotEmpty()) {
                            Text(
                                text = progressText,
                                style = FluentTheme.typography.body,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
        },
        dismissButton = {
            onCancel?.let {
                TextButton(
                    onClick = it
                ) {
                    Text(strings.fileCancel)
                }
            }
        }
    )
}
