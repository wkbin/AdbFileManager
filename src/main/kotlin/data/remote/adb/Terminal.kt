package data.remote.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import model.StringsManager
import kotlin.time.Duration.Companion.milliseconds

class Terminal(
    private val defaultTimeoutMs: Long = DEFAULT_TIMEOUT_MS
) {
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

    fun connectSafe(commandArgs: List<String>): Flow<String> = channelFlow {
        if (commandArgs.isEmpty()) {
            logWarning("Command args are empty.")
            return@channelFlow
        }

        val commandLog = commandArgs.joinToString(" ")
        logInfo(commandLog)

        val output = mutableListOf<String>()
        val process = ProcessBuilder(commandArgs).redirectErrorStream(true).start()
        try {
            withContext(Dispatchers.IO) {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        output += line
                        send(line)
                    }
                }
            }
            val exitCode = withContext(Dispatchers.IO) { process.waitFor() }
            if (exitCode != 0) {
                throw AdbCommandException(
                    exitCode,
                    output,
                    StringsManager.strings.value.terminalCommandFailed(exitCode, output.joinToString("\n"))
                )
            }
        } finally {
            if (process.isAlive) process.destroyForcibly()
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

    companion object {
        const val DEFAULT_TIMEOUT_MS = 30000L
    }
}
