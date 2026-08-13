package model

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalDestinationTest {
    @Test
    fun writableDirectoryPassesProbeWithoutLeavingFiles() {
        val directory = Files.createTempDirectory("afm-destination-").toFile()

        validateLocalDestinationDirectory(directory)

        assertEquals(emptyList(), directory.listFiles().orEmpty().toList())
        directory.deleteRecursively()
    }

    @Test
    fun missingDirectoryHasSpecificFailure() {
        val directory = File(Files.createTempDirectory("afm-missing-parent-").toFile(), "missing")

        val error = assertFailsWith<LocalDestinationException> {
            validateLocalDestinationDirectory(directory)
        }

        assertEquals(LocalDestinationFailure.NOT_FOUND, error.failure)
        directory.parentFile.deleteRecursively()
    }

    @Test
    fun regularFileIsNotAcceptedAsDirectory() {
        val file = Files.createTempFile("afm-destination-file-", ".tmp").toFile()

        val error = assertFailsWith<LocalDestinationException> {
            validateLocalDestinationDirectory(file)
        }

        assertEquals(LocalDestinationFailure.NOT_DIRECTORY, error.failure)
        file.delete()
    }
}
