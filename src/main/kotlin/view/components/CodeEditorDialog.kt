package view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import model.StringsManager

@Composable
fun CodeEditorDialog(
    visible: Boolean,
    fileName: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (content: String) -> Unit,
    fileEncoding: String,
    onEncodingChange: (String) -> Unit
) {
    if (!visible) return

    var value by remember(fileName, initialContent) {
        mutableStateOf(TextFieldValue(initialContent))
    }
    val undoStack = remember(fileName, initialContent) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(fileName, initialContent) { mutableStateListOf<TextFieldValue>() }
    var findText by remember(fileName) { mutableStateOf("") }
    var replaceText by remember(fileName) { mutableStateOf("") }
    var showFind by remember(fileName) { mutableStateOf(false) }
    var showReplace by remember(fileName) { mutableStateOf(false) }
    var matchIndex by remember(fileName) { mutableStateOf(-1) }
    var hasSearched by remember(fileName) { mutableStateOf(false) }
    var showEncodingMenu by remember { mutableStateOf(false) }
    var confirmDiscard by remember(fileName) { mutableStateOf(false) }

    val strings by StringsManager.strings.collectAsState()
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val lineHeight = 20.sp
    val lineCount = remember(value.text) { value.text.count { it == '\n' } + 1 }
    val hasChanges = value.text != initialContent
    val caret = value.selection.end.coerceIn(0, value.text.length)
    val caretLine = value.text.substring(0, caret).count { it == '\n' } + 1
    val lastNewline = if (caret == 0) -1 else value.text.lastIndexOf('\n', startIndex = caret - 1)
    val caretColumn = caret - lastNewline
    val colors = EditorSyntaxColors(
        plain = MaterialTheme.colorScheme.onSurface,
        comment = MaterialTheme.colorScheme.onSurfaceVariant,
        string = MaterialTheme.colorScheme.tertiary,
        number = MaterialTheme.colorScheme.secondary,
        keyword = MaterialTheme.colorScheme.primary,
        property = MaterialTheme.colorScheme.error,
        tag = MaterialTheme.colorScheme.primary
    )

    fun commit(next: TextFieldValue) {
        if (next.text != value.text) {
            if (undoStack.lastOrNull() != value) undoStack += value
            if (undoStack.size > HISTORY_LIMIT) undoStack.removeAt(0)
            redoStack.clear()
        }
        value = next
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack += value
        value = undoStack.removeAt(undoStack.lastIndex)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack += value
        value = redoStack.removeAt(redoStack.lastIndex)
    }

    fun findNext() {
        if (findText.isEmpty()) return
        hasSearched = true
        val start = if (value.selection.end < value.text.length) value.selection.end else 0
        var found = value.text.indexOf(findText, start, ignoreCase = true)
        if (found < 0 && start > 0) found = value.text.indexOf(findText, 0, ignoreCase = true)
        matchIndex = found
        if (found >= 0) value = value.copy(selection = androidx.compose.ui.text.TextRange(found, found + findText.length))
    }

    fun replaceCurrent() {
        val selection = value.selection
        if (!selection.collapsed && value.text.substring(selection.min, selection.max).equals(findText, ignoreCase = true)) {
            val nextText = value.text.replaceRange(selection.min, selection.max, replaceText)
            commit(TextFieldValue(nextText, androidx.compose.ui.text.TextRange(selection.min + replaceText.length)))
        } else {
            findNext()
        }
    }

    fun replaceAll() {
        if (findText.isEmpty()) return
        val regex = Regex(Regex.escape(findText), RegexOption.IGNORE_CASE)
        val replaced = regex.replace(value.text) { replaceText }
        if (replaced != value.text) commit(TextFieldValue(replaced))
    }

    fun requestClose() {
        if (hasChanges) confirmDiscard = true else onDismiss()
    }

    Window(
        state = rememberWindowState(width = 1100.dp, height = 780.dp),
        title = strings.editorTitleWithName(fileName),
        onCloseRequest = ::requestClose
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                EditorToolbar(
                    fileName = fileName,
                    language = syntaxLanguage(fileName),
                    fileEncoding = fileEncoding,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    showEncodingMenu = showEncodingMenu,
                    encodingEnabled = !hasChanges,
                    onShowEncodingMenu = { showEncodingMenu = it },
                    onEncodingChange = onEncodingChange,
                    onUndo = ::undo,
                    onRedo = ::redo,
                    onFind = { showFind = !showFind },
                    onDismiss = ::requestClose
                )

                if (showFind) {
                    FindReplaceBar(
                        findText = findText,
                        replaceText = replaceText,
                        showReplace = showReplace,
                        showNoMatch = hasSearched && matchIndex < 0,
                        onFindTextChange = { findText = it; matchIndex = -1; hasSearched = false },
                        onReplaceTextChange = { replaceText = it },
                        onFindNext = ::findNext,
                        onToggleReplace = { showReplace = !showReplace },
                        onReplace = ::replaceCurrent,
                        onReplaceAll = ::replaceAll,
                        onClose = { showFind = false }
                    )
                }

                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.width(58.dp).fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = (1..lineCount).joinToString("\n"),
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp)
                                    .verticalScroll(verticalScroll),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = lineHeight,
                                maxLines = lineCount,
                                overflow = TextOverflow.Visible
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = ::commit,
                            modifier = Modifier.fillMaxSize().padding(10.dp)
                                .verticalScroll(verticalScroll)
                                .horizontalScroll(horizontalScroll)
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when {
                                        event.isCtrlPressed && event.key == Key.S -> {
                                            if (hasChanges) onSave(value.text)
                                            true
                                        }
                                        event.isCtrlPressed && event.key == Key.Z -> { undo(); true }
                                        event.isCtrlPressed && event.key == Key.Y -> { redo(); true }
                                        event.isCtrlPressed && event.key == Key.F -> { showFind = true; true }
                                        event.key == Key.Tab -> {
                                            val selection = value.selection
                                            val next = value.text.replaceRange(selection.min, selection.max, "    ")
                                            commit(TextFieldValue(next, androidx.compose.ui.text.TextRange(selection.min + 4)))
                                            true
                                        }
                                        else -> false
                                    }
                                },
                            textStyle = TextStyle(
                                color = colors.plain,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = lineHeight
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            visualTransformation = { input ->
                                androidx.compose.ui.text.input.TransformedText(
                                    highlightedText(input.text, fileName, colors),
                                    androidx.compose.ui.text.input.OffsetMapping.Identity
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.editorPosition(caretLine, caretColumn, lineCount, value.text.length),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasChanges) Text(strings.editorModified, color = MaterialTheme.colorScheme.tertiary)
                    OutlinedButton(onClick = ::requestClose) { Text(strings.fileCancel) }
                    Button(onClick = { onSave(value.text) }, enabled = hasChanges) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.fileSave)
                    }
                }
            }

            if (confirmDiscard) {
                AlertDialog(
                    onDismissRequest = { confirmDiscard = false },
                    title = { Text(strings.editorDiscardTitle) },
                    text = { Text(strings.editorDiscardMessage) },
                    confirmButton = {
                        Button(onClick = onDismiss) { Text(strings.editorDiscard) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDiscard = false }) { Text(strings.editorContinueEditing) }
                    }
                )
            }
        }
    }
}

@Composable
private fun EditorToolbar(
    fileName: String,
    language: String,
    fileEncoding: String,
    canUndo: Boolean,
    canRedo: Boolean,
    showEncodingMenu: Boolean,
    encodingEnabled: Boolean,
    onShowEncodingMenu: (Boolean) -> Unit,
    onEncodingChange: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFind: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings by StringsManager.strings.collectAsState()
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(fileName, style = MaterialTheme.typography.titleMedium)
                Text(language, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUndo, enabled = canUndo) { Icon(Icons.AutoMirrored.Outlined.Undo, strings.editorUndo) }
            IconButton(onClick = onRedo, enabled = canRedo) { Icon(Icons.AutoMirrored.Outlined.Redo, strings.editorRedo) }
            IconButton(onClick = onFind) { Icon(Icons.Outlined.Search, strings.editorFind) }
            Box {
                TextButton(
                    onClick = { onShowEncodingMenu(true) },
                    enabled = encodingEnabled
                ) { Text(fileEncoding) }
                DropdownMenu(expanded = showEncodingMenu, onDismissRequest = { onShowEncodingMenu(false) }) {
                    ENCODINGS.forEach { encoding ->
                        DropdownMenuItem(text = { Text(encoding) }, onClick = {
                            onEncodingChange(encoding)
                            onShowEncodingMenu(false)
                        })
                    }
                }
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, strings.editorClose) }
        }
    }
}

@Composable
private fun FindReplaceBar(
    findText: String,
    replaceText: String,
    showReplace: Boolean,
    showNoMatch: Boolean,
    onFindTextChange: (String) -> Unit,
    onReplaceTextChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onToggleReplace: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    val strings by StringsManager.strings.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(findText, onFindTextChange, label = { Text(strings.editorFind) }, singleLine = true, modifier = Modifier.width(240.dp))
        IconButton(onClick = onFindNext, enabled = findText.isNotEmpty()) { Icon(Icons.Outlined.Search, strings.editorFindNext) }
        IconButton(onClick = onToggleReplace) { Icon(Icons.Outlined.FindReplace, strings.editorReplace) }
        if (showReplace) {
            OutlinedTextField(replaceText, onReplaceTextChange, label = { Text(strings.editorReplaceWith) }, singleLine = true, modifier = Modifier.width(240.dp))
            TextButton(onClick = onReplace, enabled = findText.isNotEmpty()) { Text(strings.editorReplace) }
            TextButton(onClick = onReplaceAll, enabled = findText.isNotEmpty()) { Text(strings.editorReplaceAll) }
        }
        if (findText.isNotEmpty() && showNoMatch) Text(strings.editorNoMatch, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, strings.editorCloseFind) }
    }
}

private val ENCODINGS = listOf("UTF-8", "GBK", "GB18030", "GB2312", "UTF-16", "UTF-32", "ISO-8859-1", "BIG5", "Shift-JIS", "EUC-JP", "EUC-KR")
private const val HISTORY_LIMIT = 100
