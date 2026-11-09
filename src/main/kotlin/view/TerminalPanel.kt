package view

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Delete

import io.github.composefluent.component.Text
import io.github.composefluent.component.Icon
import view.fluent.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.github.composefluent.FluentTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.remote.adb.AdbShellSession
import data.remote.adb.TerminalMode
import model.StringsManager
import view.components.dnd.adbDndTarget
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun TerminalPanel(session: AdbShellSession) {
    val strings by StringsManager.strings.collectAsState()
    val state by session.state.collectAsState()
    val colors = FluentTheme.colors
    val textColor = colors.text.text.primary
    val darkTheme = colors.darkMode
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var command by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(emptyList<String>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }

    fun submit() {
        val normalized = command.trim()
        if (normalized.isEmpty()) return
        session.send(normalized)
        history = history + normalized
        historyIndex = -1
        command = ""
    }

    fun previousCommand() {
        if (history.isEmpty()) return
        historyIndex = if (historyIndex < 0) history.lastIndex else (historyIndex - 1).coerceAtLeast(0)
        command = history[historyIndex]
    }

    fun nextCommand() {
        if (historyIndex < 0) return
        if (historyIndex >= history.lastIndex) {
            historyIndex = -1
            command = ""
        } else {
            historyIndex += 1
            command = history[historyIndex]
        }
    }

    LaunchedEffect(state.output.length) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    LaunchedEffect(session) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .adbDndTarget { files ->
                command = appendDroppedFilePaths(command, files)
                historyIndex = -1
                focusRequester.requestFocus()
            },
        color = colors.background.solid.base
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(colors.background.solid.tertiary).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.device.deviceId,
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = if (state.mode == TerminalMode.SHELL) "  SHELL" else "  ADB",
                    color = if (state.mode == TerminalMode.SHELL) colors.system.success else colors.text.accent.primary,
                    fontFamily = FontFamily.Monospace
                )
                Box(Modifier.weight(1f))
                IconButton(onClick = session::clear) {
                    Icon(
                        Icons.Regular.Delete,
                        contentDescription = strings.terminalClear,
                        tint = textColor
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState).padding(12.dp)
            ) {
                SelectionContainer {
                    val annotatedOutput = remember(state.output, textColor, darkTheme) {
                        view.components.AnsiTextParser.parse(state.output, defaultColor = textColor, darkTheme = darkTheme)
                    }
                    Text(
                        text = annotatedOutput,
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(colors.background.solid.base).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.prompt(),
                    color = if (state.mode == TerminalMode.SHELL) colors.system.success else colors.text.accent.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
                BasicTextField(
                    value = command,
                    onValueChange = {
                        command = it.replace("\n", "")
                        historyIndex = -1
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.Enter -> {
                                    submit()
                                    true
                                }
                                Key.DirectionUp -> {
                                    previousCommand()
                                    true
                                }
                                Key.DirectionDown -> {
                                    nextCommand()
                                    true
                                }
                                Key.Tab -> {
                                    coroutineScope.launch {
                                        val completion = session.complete(command)
                                        command = completion.command
                                        if (completion.candidates.isNotEmpty()) {
                                            session.showCompletionCandidates(completion.candidates)
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        },
                    textStyle = TextStyle(
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(textColor),
                    singleLine = true
                )
            }
        }
    }
}

internal fun appendDroppedFilePaths(command: String, files: List<File>): String {
    if (files.isEmpty()) return command
    val paths = files.joinToString(" ") { file -> quoteTerminalPath(file.absolutePath) }
    return when {
        command.isEmpty() -> paths
        command.last().isWhitespace() -> command + paths
        else -> "$command $paths"
    }
}

private fun quoteTerminalPath(path: String): String =
    "\"${path.replace("\"", "\\\"")}\""
