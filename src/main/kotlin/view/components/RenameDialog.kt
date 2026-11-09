package view.components

import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Dismiss
import io.github.composefluent.icons.regular.Edit
import io.github.composefluent.icons.regular.TextFont

import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text
import io.github.composefluent.component.Icon
import view.fluent.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import model.FileItem
import model.StringsManager

@Composable
fun RenameDialog(
    visible: Boolean,
    file: FileItem,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    if (!visible) return
    val strings by StringsManager.strings.collectAsState()
    var newName by remember { mutableStateOf(file.fileName) }
    val isInputValid = newName.isNotEmpty() && !newName.contains("/") && !newName.contains("\\")
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(visible) {
        delay(100)
        focusRequester.requestFocus()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = FluentTheme.shapes.overlay,
            color = FluentTheme.colors.background.solid.tertiary
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Regular.Edit,
                        contentDescription = null,
                        tint = FluentTheme.colors.text.accent.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = strings.fileRename,
                        style = FluentTheme.typography.subtitle.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = FluentTheme.colors.text.text.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Regular.Dismiss,
                            contentDescription = strings.editorClose,
                            tint = FluentTheme.colors.text.text.secondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(strings.fileName) },
                    singleLine = true,
                    isError = newName.isNotEmpty() && !isInputValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onKeyEvent {
                            if (it.key == Key.Enter && isInputValid) {
                                onConfirm(newName)
                                true
                            } else {
                                false
                            }
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isInputValid) {
                                focusManager.clearFocus()
                                onConfirm(newName)
                            }
                        }
                    ),
                    supportingText = {
                        if (newName.isNotEmpty() && !isInputValid) {
                            Text(
                                strings.fileNameInvalidError,
                                color = FluentTheme.colors.system.critical
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Regular.TextFont,
                            contentDescription = null
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = FluentTheme.colors.text.text.secondary
                        )
                    ) {
                        Text(strings.fileCancel)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = { 
                            if (isInputValid) {
                                onConfirm(newName)
                            }
                        },
                        enabled = isInputValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FluentTheme.colors.text.accent.primary
                        )
                    ) {
                        Text(strings.fileSave)
                    }
                }
            }
        }
    }
}
