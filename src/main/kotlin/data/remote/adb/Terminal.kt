package data.remote.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.concurrent.TimeUnit
import model.StringsManager
import org.jetbrains.skiko.hostOs
import kotlin.time.Duration.Companion.milliseconds

class Terminal(
    private val defaultTimeoutMs: Long = DEFAULT_TIMEOUT_MS
) {
    @Deprecated(
        message = "Use runSafe() with List<String> args instead. String-based commands break with paths containing spaces.",
        replaceWith = ReplaceWith("runSafe(command.split(\" \"), throwOnError, timeoutMs)")
    )
    suspend fun run(
        command: String,
        throwOnError: Boolean = false,
        timeoutMs: Long = defaultTimeoutMs
    ): List<String> = withContext(Dispatchers.IO) {
        if (command.isEmpty()) {
            logWarning("Command is null or empty.")
            return@withContext emptyList()
        }
        // WARNING: split(" ") breaks quoted arguments with spaces.
        // Use runSafe() which accepts List<String> args.
        val commandArray = if (hostOs.isWindows) {
            command.split(" ").toTypedArray()
        } else {
            val shell = System.getenv("SHELL")
            arrayOf(shell, "-c", command)
        }

        executeProcess(commandArray.toList(), command, throwOnError, timeoutMs)
    }

    suspend fun runSafe(
        commandArgs: List<String>,
        throwOnError: Boolean = false,
        timeoutMs: Long = defaultTimeoutMs
    ): List<String> = withContext(Dispatchers.IO) {
        if (commandArgs.isEmpty()) {
            logWarning("Command args are empty.")
            return@withContext emptyList()
        }

        executeProcess(commandArgs, commandArgs.joinToString(" "), throwOnError, timeoutMs)
    }

    private suspend fun executeProcess(
        commandArgs: List<String>,
        commandLog: String,
        throwOnError: Boolean,
        timeoutMs: Long
    ): List<String> {
        var process: java.lang.Process? = null
        return try {
            withTimeout(timeoutMs.milliseconds) {
                process = ProcessBuilder(commandArgs)
                    .redirectErrorStream(true)
                    .start()

                val output = process!!.inputStream.bufferedReader().readLines()

                if (throwOnError) {
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        val msg = output.joinToString("\n")
                        throw AdbCommandException(
                            exitCode,
                            output,
                            StringsManager.strings.value.terminalCommandFailed(exitCode, msg)
                        )
                    }
                }

                output.also {
                    logCommandOutput(commandLog, it)
                }
            }
        } catch (e: TimeoutCancellationException) {
            process?.destroyForcibly()
            throw AdbException("Command timed out after ${timeoutMs}ms", e)
        } catch (e: Exception) {
            process?.destroyForcibly()
            throw e
        }
    }

    @Deprecated(
        message = "Use connectSafe() with List<String> args instead. String-based commands break with paths containing spaces.",
        replaceWith = ReplaceWith("connectSafe(command.split(\" \"))")
    )
    fun connect(command: String): Flow<String> = channelFlow {
        if (command.isEmpty()) {
            logWarning("Command is null or empty.")
            return@channelFlow
        }

        logInfo(command)

        // WARNING: split(" ") breaks quoted arguments with spaces.
        // Use connectSafe() which accepts List<String> args.
        val process = ProcessBuilder(*command.split(" ").toTypedArray())
            .redirectErrorStream(true)
            .start()

        withContext(Dispatchers.IO) {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    try {
                        send(line)
                    } catch (e: Exception) {
                        logError("Error sending line to flow", e)
                        cancel()
                    }
                }
            }
        }

        awaitClose {
            process.destroy()
            logInfo("""COMPLETED EXECUTE ADB\n$command""".trimIndent())
        }
    }.flowOn(Dispatchers.IO)

    fun connectSafe(commandArgs: List<String>): Flow<String> = channelFlow {
        if (commandArgs.isEmpty()) {
            logWarning("Command args are empty.")
            return@channelFlow
        }

        val commandLog = commandArgs.joinToString(" ")
        logInfo(commandLog)

        val process = ProcessBuilder(commandArgs)
            .redirectErrorStream(true)
            .start()

        withContext(Dispatchers.IO) {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    try {
                        send(line)
                    } catch (e: Exception) {
                        logError("Error sending line to flow", e)
                        cancel()
                    }
                }
            }
        }

        awaitClose {
            process.destroy()
            logInfo("""COMPLETED EXECUTE ADB\n$commandLog""".trimIndent())
        }
    }.flowOn(Dispatchers.IO)



    private fun logCommandOutput(command: String, output: List<String>) {
        println(
            """
                -- ADB OUTPUT --
                EXECUTE: $command
                OUTPUT: $output
                -- END --
            """.trimIndent()
        )
    }

    private fun logInfo(message: String) {
        println("[INFO] $message")
    }

    private fun logWarning(message: String) {
        println("[WARNING] $message")
    }

    private fun logError(message: String, cause: Throwable? = null) {
        println("[ERROR] $message")
        cause?.printStackTrace()
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 30000L
    }
}
