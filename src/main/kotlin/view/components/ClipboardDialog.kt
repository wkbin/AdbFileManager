package view.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ClipboardDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPush: (text: String) -> Unit
) {
    if (!visible) return

    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发送到设备剪贴板") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("输入要写入 Android 剪贴板的文本。")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(top = 12.dp),
                    placeholder = { Text("剪贴板内容") },
                    minLines = 5,
                    maxLines = 12
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPush(text) },
                enabled = text.isNotBlank()
            ) { Text("发送") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
