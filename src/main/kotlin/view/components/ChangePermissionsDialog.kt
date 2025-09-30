package view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.FileItem
import model.StringsManager

@Composable
fun ChangePermissionsDialog(
    visible: Boolean,
    file: FileItem?,
    onDismiss: () -> Unit,
    onConfirm: (fileName: String, permissions: String) -> Unit
) {
    if (!visible || file == null) return

    val strings by StringsManager.strings.collectAsState()
    var permissionsText by remember(file.permissions) { 
        mutableStateOf(permissionsToOctal(file.permissions)) 
    }
    var useOctal by remember { mutableStateOf(true) }

    var readOwner by remember { mutableStateOf(file.permissions.getOrNull(1) == 'r') }
    var writeOwner by remember { mutableStateOf(file.permissions.getOrNull(2) == 'w') }
    var executeOwner by remember { mutableStateOf(file.permissions.getOrNull(3) == 'x') }
    
    var readGroup by remember { mutableStateOf(file.permissions.getOrNull(4) == 'r') }
    var writeGroup by remember { mutableStateOf(file.permissions.getOrNull(5) == 'w') }
    var executeGroup by remember { mutableStateOf(file.permissions.getOrNull(6) == 'x') }
    
    var readOther by remember { mutableStateOf(file.permissions.getOrNull(7) == 'r') }
    var writeOther by remember { mutableStateOf(file.permissions.getOrNull(8) == 'w') }
    var executeOther by remember { mutableStateOf(file.permissions.getOrNull(9) == 'x') }

    fun updatePermissionsFromCheckboxes() {
        val owner = (if (readOwner) "r" else "-") +
                (if (writeOwner) "w" else "-") +
                (if (executeOwner) "x" else "-")
        val group = (if (readGroup) "r" else "-") +
                (if (writeGroup) "w" else "-") +
                (if (executeGroup) "x" else "-")
        val other = (if (readOther) "r" else "-") +
                (if (writeOther) "w" else "-") +
                (if (executeOther) "x" else "-")
        permissionsText = permissionsToOctal("-rwxrwxrwx".substring(0, 1) + owner + group + other)
    }

    fun updateCheckboxesFromPermissions() {
        val symbolic = octalToSymbolic(permissionsText)
        readOwner = symbolic.getOrNull(1) == 'r'
        writeOwner = symbolic.getOrNull(2) == 'w'
        executeOwner = symbolic.getOrNull(3) == 'x'
        readGroup = symbolic.getOrNull(4) == 'r'
        writeGroup = symbolic.getOrNull(5) == 'w'
        executeGroup = symbolic.getOrNull(6) == 'x'
        readOther = symbolic.getOrNull(7) == 'r'
        writeOther = symbolic.getOrNull(8) == 'w'
        executeOther = symbolic.getOrNull(9) == 'x'
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.editorTitleWithName(file.fileName),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.permissionCurrent(file.permissions))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useOctal,
                            onCheckedChange = { 
                                useOctal = it
                                if (it) {
                                    updatePermissionsFromCheckboxes()
                                } else {
                                    updateCheckboxesFromPermissions()
                                }
                            }
                        )
                        Text(strings.permissionUseOctal)
                    }
                }

                if (useOctal) {
                    OutlinedTextField(
                        value = permissionsText,
                        onValueChange = { 
                            permissionsText = it
                            updateCheckboxesFromPermissions()
                        },
                        label = { Text(strings.permissionOctalLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PermissionRow(
                            title = strings.permissionOwner,
                            readLabel = strings.permissionRead,
                            writeLabel = strings.permissionWrite,
                            executeLabel = strings.permissionExecute,
                            read = readOwner, write = writeOwner, execute = executeOwner,
                            onReadChange = { readOwner = it; updatePermissionsFromCheckboxes() },
                            onWriteChange = { writeOwner = it; updatePermissionsFromCheckboxes() },
                            onExecuteChange = { executeOwner = it; updatePermissionsFromCheckboxes() }
                        )
                        PermissionRow(
                            title = strings.permissionGroup,
                            readLabel = strings.permissionRead,
                            writeLabel = strings.permissionWrite,
                            executeLabel = strings.permissionExecute,
                            read = readGroup, write = writeGroup, execute = executeGroup,
                            onReadChange = { readGroup = it; updatePermissionsFromCheckboxes() },
                            onWriteChange = { writeGroup = it; updatePermissionsFromCheckboxes() },
                            onExecuteChange = { executeGroup = it; updatePermissionsFromCheckboxes() }
                        )
                        PermissionRow(
                            title = strings.permissionOther,
                            readLabel = strings.permissionRead,
                            writeLabel = strings.permissionWrite,
                            executeLabel = strings.permissionExecute,
                            read = readOther, write = writeOther, execute = executeOther,
                            onReadChange = { readOther = it; updatePermissionsFromCheckboxes() },
                            onWriteChange = { writeOther = it; updatePermissionsFromCheckboxes() },
                            onExecuteChange = { executeOther = it; updatePermissionsFromCheckboxes() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(file.fileName, permissionsText) }
            ) {
                Text(strings.fileSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.fileCancel)
            }
        }
    )
}

@Composable
private fun PermissionRow(
    title: String,
    readLabel: String,
    writeLabel: String,
    executeLabel: String,
    read: Boolean,
    write: Boolean,
    execute: Boolean,
    onReadChange: (Boolean) -> Unit,
    onWriteChange: (Boolean) -> Unit,
    onExecuteChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = read, onCheckedChange = onReadChange)
                Text(readLabel)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = write, onCheckedChange = onWriteChange)
                Text(writeLabel)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = execute, onCheckedChange = onExecuteChange)
                Text(executeLabel)
            }
        }
    }
}

private fun permissionsToOctal(symbolic: String): String {
    if (symbolic.length < 10) return "755"
    
    fun toBit(r: Boolean, w: Boolean, x: Boolean): Int {
        return (if (r) 4 else 0) + (if (w) 2 else 0) + (if (x) 1 else 0)
    }
    
    val owner = toBit(
        symbolic.getOrNull(1) == 'r',
        symbolic.getOrNull(2) == 'w',
        symbolic.getOrNull(3) == 'x'
    )
    val group = toBit(
        symbolic.getOrNull(4) == 'r',
        symbolic.getOrNull(5) == 'w',
        symbolic.getOrNull(6) == 'x'
    )
    val other = toBit(
        symbolic.getOrNull(7) == 'r',
        symbolic.getOrNull(8) == 'w',
        symbolic.getOrNull(9) == 'x'
    )
    
    return "$owner$group$other"
}

private fun octalToSymbolic(octal: String): String {
    val clean = octal.filter { it.isDigit() }.padStart(3, '0')
    if (clean.length < 3) return "-rwxr-xr-x"
    
    fun fromBit(bit: Int): String {
        val r = if (bit and 4 != 0) 'r' else '-'
        val w = if (bit and 2 != 0) 'w' else '-'
        val x = if (bit and 1 != 0) 'x' else '-'
        return "$r$w$x"
    }
    
    val owner = fromBit(clean[0].digitToIntOrNull() ?: 7)
    val group = fromBit(clean[1].digitToIntOrNull() ?: 5)
    val other = fromBit(clean[2].digitToIntOrNull() ?: 5)
    
    return "-$owner$group$other"
}
