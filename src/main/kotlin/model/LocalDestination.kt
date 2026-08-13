package model

import java.io.File
import java.io.IOException
import java.nio.file.Files

enum class LocalDestinationFailure {
    NOT_FOUND,
    NOT_DIRECTORY,
    NOT_WRITABLE
}

class LocalDestinationException(
    val failure: LocalDestinationFailure,
    val directory: File,
    cause: Throwable? = null
) : IOException("Local destination is not writable: ${directory.absolutePath}", cause)

/**
 * Fast, side-effect-free check used to disable transfer controls in the UI.
 * A definitive create/delete probe is still performed immediately before export.
 */
fun isLikelyWritableLocalDirectory(directory: File): Boolean =
    directory.isDirectory && Files.isWritable(directory.toPath())

/**
 * Verifies that a destination can actually accept a file. Files.isWritable alone
 * is not reliable for every Windows ACL, so use a uniquely named probe and remove it.
 */
fun validateLocalDestinationDirectory(directory: File) {
    val target = directory.absoluteFile
    if (!target.exists()) {
        throw LocalDestinationException(LocalDestinationFailure.NOT_FOUND, target)
    }
    if (!target.isDirectory) {
        throw LocalDestinationException(LocalDestinationFailure.NOT_DIRECTORY, target)
    }

    var probe: java.nio.file.Path? = null
    try {
        val createdProbe = Files.createTempFile(target.toPath(), ".adb-file-manager-write-test-", ".tmp")
        probe = createdProbe
        Files.delete(createdProbe)
        probe = null
    } catch (error: Exception) {
        throw LocalDestinationException(LocalDestinationFailure.NOT_WRITABLE, target, error)
    } finally {
        probe?.let { runCatching { Files.deleteIfExists(it) } }
    }
}
