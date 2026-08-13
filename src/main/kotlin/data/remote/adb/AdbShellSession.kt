package data.remote.adb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.Closeable
import java.io.OutputStreamWriter
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

enum class TerminalMode {
    ADB,
    SHELL
}

data class AdbShellSessionState(
    val output: String = "",
    val mode: TerminalMode = TerminalMode.ADB,
    val currentDirectory: String = "/",
    val closed: Boolean = false
)

data class TerminalCompletion(
    val command: String,
    val candidates: List<String> = emptyList()
)

/**
 * A terminal session that starts in ADB mode. `adb shell` opens a persistent
 * device shell; `exit` closes that shell and returns to ADB mode.
 */
class AdbShellSession internal constructor(
    private val adbPath: String,
    val device: AdbDevice
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isClosed = AtomicBoolean(false)
    private val childProcesses = Collections.synchronizedSet(mutableSetOf<Process>())
    private val writerLock = Any()

    private val _state = MutableStateFlow(
        AdbShellSessionState(
            output = "ADB terminal for ${device.deviceId}\nType adb shell to enter the device shell.\n"
        )
    )
    val state: StateFlow<AdbShellSessionState> = _state.asStateFlow()

    @Volatile
    private var shellProcess: Process? = null

    @Volatile
    private var shellWriter: BufferedWriter? = null

    fun send(command: String) {
        val normalizedCommand = command.trim()
        if (normalizedCommand.isEmpty() || isClosed.get()) return

        appendOutput("${prompt()}$normalizedCommand\n")

        if (normalizedCommand.equals("clear", ignoreCase = true)) {
            clear()
            return
        }

        when (_state.value.mode) {
            TerminalMode.SHELL -> sendToShell(normalizedCommand)
            TerminalMode.ADB -> sendAdbModeCommand(normalizedCommand)
        }
    }

    fun clear() {
        _state.update { it.copy(output = "") }
    }

    fun showCompletionCandidates(candidates: List<String>) {
        if (candidates.isNotEmpty()) appendOutput(candidates.joinToString("  ", postfix = "\n"))
    }

    fun prompt(): String = when (_state.value.mode) {
        TerminalMode.ADB -> "\$ "
        TerminalMode.SHELL -> "${_state.value.currentDirectory} \$ "
    }

    suspend fun complete(command: String): TerminalCompletion = withContext(Dispatchers.IO) {
        if (_state.value.mode != TerminalMode.SHELL) return@withContext TerminalCompletion(command)

        val token = findCompletionToken(command)
        val pathPrefix = token.value
        val slashIndex = pathPrefix.lastIndexOf('/')
        val directoryPart = if (slashIndex >= 0) pathPrefix.substring(0, slashIndex + 1) else ""
        val namePrefix = if (slashIndex >= 0) pathPrefix.substring(slashIndex + 1) else pathPrefix
        val directory = resolveCompletionDirectory(_state.value.currentDirectory, directoryPart)
        val names = listDirectory(directory)
            .filter { it.startsWith(namePrefix) }
            .sorted()

        if (names.isEmpty()) return@withContext TerminalCompletion(command)

        val commonName = longestCommonPrefix(names)
        val completedPath = directoryPart + commonName
        val unique = names.size == 1
        val uniquePath = if (unique) directoryPart + names.single() else completedPath
        val isDirectory = unique && isRemoteDirectory(resolveRemotePath(_state.value.currentDirectory, uniquePath))
        val replacement = when {
            !unique -> quoteForShellInput(completedPath)
            isDirectory -> quoteForShellInput(uniquePath) + "/"
            else -> quoteForShellInput(uniquePath) + " "
        }
        val completedCommand = command.replaceRange(token.startIndex, command.length, replacement)

        TerminalCompletion(
            command = completedCommand,
            candidates = if (names.size > 1) names.map { directoryPart + it } else emptyList()
        )
    }

    private fun sendAdbModeCommand(command: String) {
        val adbShellCommand = ADB_SHELL_REGEX.matchEntire(command)
        when {
            adbShellCommand != null && adbShellCommand.groupValues[1].isBlank() -> startShell()
            adbShellCommand != null -> runAdbCommand(command)
            isAdbCommand(command) -> runAdbCommand(command)
            else -> appendOutput("ADB mode only accepts adb commands. Type adb shell to enter the device shell.\n")
        }
    }

    private fun startShell() {
        if (shellProcess?.isAlive == true) return

        scope.launch {
            try {
                appendOutput("Entering device shell...\n")
                val process = ProcessBuilder(adbPath, "-s", device.deviceId, "shell")
                    .redirectErrorStream(true)
                    .start()
                shellProcess = process
                shellWriter = BufferedWriter(OutputStreamWriter(process.outputStream))
                _state.update { it.copy(mode = TerminalMode.SHELL) }
                writeShellLine(PWD_COMMAND)
                streamShellOutput(process)
                val exitCode = process.waitFor()
                if (!isClosed.get() && exitCode != 0) {
                    appendOutput("\nShell exited with code $exitCode.\n")
                }
            } catch (exception: Exception) {
                if (!isClosed.get()) {
                    appendOutput("Unable to start shell: ${exception.message ?: "Unknown error"}\n")
                }
            } finally {
                shellWriter = null
                shellProcess = null
                if (!isClosed.get()) {
                    _state.update { it.copy(mode = TerminalMode.ADB, currentDirectory = "/") }
                    appendOutput("Returned to ADB mode.\n")
                }
            }
        }
    }

    private fun sendToShell(command: String) {
        scope.launch {
            val writer = shellWriter
            if (writer == null || shellProcess?.isAlive != true) {
                appendOutput("Shell is not ready.\n")
                return@launch
            }

            try {
                writeShellLine(command)
                if (!command.equals("exit", ignoreCase = true)) writeShellLine(PWD_COMMAND)
            } catch (exception: Exception) {
                if (!isClosed.get()) {
                    appendOutput("Failed to send command: ${exception.message ?: "Unknown error"}\n")
                }
            }
        }
    }

    private fun runAdbCommand(command: String) {
        scope.launch {
            var process: Process? = null
            try {
                val arguments = CommandLineTokenizer.tokenize(command).drop(1)
                process = ProcessBuilder(listOf(adbPath) + arguments)
                    .redirectErrorStream(true)
                    .start()
                childProcesses += process
                streamOutput(process)
                val exitCode = process.waitFor()
                if (exitCode != 0 && !isClosed.get()) {
                    appendOutput("\nadb exited with code $exitCode.\n")
                }
            } catch (exception: Exception) {
                if (!isClosed.get()) {
                    appendOutput("Failed to run adb: ${exception.message ?: "Unknown error"}\n")
                }
            } finally {
                process?.let { childProcesses -= it }
            }
        }
    }

    private fun streamOutput(process: Process) {
        process.inputStream.bufferedReader().use { reader ->
            val buffer = CharArray(OUTPUT_BUFFER_SIZE)
            while (!isClosed.get()) {
                val count = reader.read(buffer)
                if (count < 0) break
                appendOutput(String(buffer, 0, count))
            }
        }
    }

    private fun streamShellOutput(process: Process) {
        process.inputStream.bufferedReader().useLines { lines ->
            var pendingLine: String? = null
            lines.forEach { line ->
                if (line.startsWith(PWD_MARKER)) {
                    pendingLine?.takeIf { it.isNotEmpty() }?.let { appendOutput("$it\n") }
                    pendingLine = null
                    val directory = line.removePrefix(PWD_MARKER).trim().ifEmpty { "/" }
                    _state.update { it.copy(currentDirectory = directory) }
                } else {
                    pendingLine?.let { appendOutput("$it\n") }
                    pendingLine = line
                }
            }
            pendingLine?.let { appendOutput("$it\n") }
        }
    }

    private fun writeShellLine(command: String) {
        val writer = shellWriter ?: return
        synchronized(writerLock) {
            writer.write(command)
            writer.newLine()
            writer.flush()
        }
    }

    private fun listDirectory(directory: String): List<String> {
        val process = ProcessBuilder(
            adbPath,
            "-s",
            device.deviceId,
            "shell",
            "sh",
            "-c",
            "ls -1 -a -- ${shellQuote(directory)}"
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readLines()
        val exitCode = process.waitFor()
        if (exitCode != 0) return emptyList()
        return output.filterNot { it == "." || it == ".." }
    }

    private fun isRemoteDirectory(path: String): Boolean {
        val process = ProcessBuilder(
            adbPath,
            "-s",
            device.deviceId,
            "shell",
            "sh",
            "-c",
            "test -d ${shellQuote(path)}"
        ).redirectErrorStream(true).start()
        process.inputStream.close()
        return process.waitFor() == 0
    }

    private fun appendOutput(text: String) {
        if (text.isEmpty()) return
        _state.update { state ->
            val combined = state.output + text
            state.copy(
                output = if (combined.length > MAX_OUTPUT_CHARS) {
                    combined.takeLast(MAX_OUTPUT_CHARS)
                } else {
                    combined
                }
            )
        }
    }

    private fun isAdbCommand(command: String): Boolean =
        command.equals("adb", ignoreCase = true) ||
            (command.length > 3 &&
                command.regionMatches(0, "adb", 0, 3, ignoreCase = true) &&
                command[3].isWhitespace())

    override fun close() {
        if (!isClosed.compareAndSet(false, true)) return
        synchronized(writerLock) {
            runCatching { shellWriter?.close() }
        }
        shellProcess?.destroyForcibly()
        synchronized(childProcesses) {
            childProcesses.forEach { it.destroyForcibly() }
            childProcesses.clear()
        }
        _state.update { it.copy(closed = true) }
        scope.cancel()
    }

    companion object {
        private val ADB_SHELL_REGEX = Regex("(?i)^adb\\s+shell(?:\\s+(.*))?$")
        private const val PWD_MARKER = "__AFM_PWD__="
        private const val PWD_COMMAND = "printf '\\n${PWD_MARKER}%s\\n' \"\$PWD\""
        private const val OUTPUT_BUFFER_SIZE = 1024
        private const val MAX_OUTPUT_CHARS = 500_000
    }
}

internal data class CompletionToken(val startIndex: Int, val value: String)

internal fun findCompletionToken(command: String): CompletionToken {
    var quote: Char? = null
    var tokenStart = 0
    command.forEachIndexed { index, char ->
        when {
            quote != null && char == quote -> quote = null
            quote == null && (char == '\'' || char == '"') -> {
                quote = char
                tokenStart = index
            }
            quote == null && char.isWhitespace() -> tokenStart = index + 1
        }
    }
    val raw = command.substring(tokenStart)
    val value = raw.removePrefix("\"").removePrefix("'")
    return CompletionToken(tokenStart, value)
}

internal fun longestCommonPrefix(values: List<String>): String {
    if (values.isEmpty()) return ""
    var prefix = values.first()
    values.drop(1).forEach { value ->
        while (!value.startsWith(prefix) && prefix.isNotEmpty()) prefix = prefix.dropLast(1)
    }
    return prefix
}

private fun resolveCompletionDirectory(currentDirectory: String, directoryPart: String): String = when {
    directoryPart.startsWith("/") -> directoryPart.ifEmpty { "/" }
    directoryPart.isEmpty() -> currentDirectory
    else -> resolveRemotePath(currentDirectory, directoryPart)
}.trimEnd('/').ifEmpty { "/" }

private fun resolveRemotePath(currentDirectory: String, path: String): String =
    if (path.startsWith('/')) path else "${currentDirectory.trimEnd('/')}/$path"

private fun quoteForShellInput(path: String): String = when {
    path.isEmpty() -> path
    path.none { it.isWhitespace() || it == '\'' || it == '"' } -> path
    else -> "'${path.replace("'", "'\\''")}'"
}

private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

class AdbShellSessionFactory(private val adb: Adb) {
    fun create(device: AdbDevice): AdbShellSession = AdbShellSession(adb.getAdbPath(), device)
}

/** Splits ADB arguments without invoking the host operating system shell. */
object CommandLineTokenizer {
    fun tokenize(command: String): List<String> {
        if (command.isBlank()) return emptyList()

        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var tokenStarted = false
        var index = 0

        while (index < command.length) {
            val char = command[index]
            when {
                char == '\\' && quote != '\'' -> {
                    val next = command.getOrNull(index + 1)
                    if (next == '\\' || next == '"' || (quote == null && next?.isWhitespace() == true)) {
                        current.append(next)
                        index += 1
                    } else {
                        current.append(char)
                    }
                    tokenStarted = true
                }

                quote != null && char == quote -> {
                    quote = null
                    tokenStarted = true
                }

                quote != null -> {
                    current.append(char)
                    tokenStarted = true
                }

                char == '\'' || char == '"' -> {
                    quote = char
                    tokenStarted = true
                }

                char.isWhitespace() -> {
                    if (tokenStarted) {
                        result += current.toString()
                        current.clear()
                        tokenStarted = false
                    }
                }

                else -> {
                    current.append(char)
                    tokenStarted = true
                }
            }
            index += 1
        }

        require(quote == null) { "Unclosed quote in command" }
        if (tokenStarted) result += current.toString()
        return result
    }
}
