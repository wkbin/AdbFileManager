package runtime.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.hostOs
import java.io.File

class Terminal {
    suspend fun run(command: String): List<String> = withContext(Dispatchers.IO) {
        if (command.isEmpty()) {
            println("Command is null or empty.")
            return@withContext emptyList()
        }
        val commandArray = if (hostOs.isWindows) {
            command.split(" ").toTypedArray()
        } else {
            val shell = System.getenv("SHELL")
            arrayOf(shell, "-c", command)
        }
        val process = ProcessBuilder(*commandArray)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readLines()
        output.also {
            println(
                """$command $it """.trimIndent()
            )
        }
    }

    fun connect(command: String): Flow<String> = channelFlow {
        if (command.isEmpty()) {
            println("Command is null or empty.")
            return@channelFlow
        }

        println(command)

        val process = ProcessBuilder(*command.split(" ").toTypedArray())
            .redirectErrorStream(true)
            .start()

        // We need to launch a new coroutine for reading the process's output, as it's a blocking operation
        withContext(Dispatchers.IO) {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach {
                    try {
                        // Emit each line to the flow
                        send(it)
                    } catch (e: Exception) {
                        // If anything goes wrong, cancel the flow
                        cancel()
                    }
                }
            }
        }

        awaitClose {
            // Close the process when the flow is collected
            process.destroy()
            println(
                """COMPLETED EXECUTE ADB
                $command
            """.trimIndent()
            )
        }
    }.flowOn(Dispatchers.IO)
}