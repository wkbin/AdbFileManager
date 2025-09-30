package view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import model.StringsManager

@Composable
fun TransferProgressDialog(
    visible: Boolean,
    fileName: String?,
    onDismiss: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    if (!visible) return
    
    val strings by StringsManager.strings.collectAsState()
    var progress by remember { mutableStateOf(0f) }
    var fileSizeText by remember { mutableStateOf(strings.transferPreparing) }

    LaunchedEffect(visible) {
        progress = 0f
        while (progress < 0.95f) {
            delay(100)
            progress += (0.01f + (0.02f * (1f - progress))).coerceIn(0.01f, 0.05f)
            fileSizeText = when {
                progress < 0.25f -> strings.transferPreparing
                progress < 0.5f -> strings.transferInProgress(25)
                progress < 0.75f -> strings.transferInProgress(50)
                else -> strings.transferInProgress(75)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = strings.transferFile,
                style = MaterialTheme.typography.titleLarge
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fileSizeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
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
