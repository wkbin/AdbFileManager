package utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZipUtilsTest {
    @Test
    fun archiveMatchesOnlyWhenAllInstalledFilesMatch() {
        val archive = zipOf(
            "adb.exe" to "adb-binary".encodeToByteArray(),
            "AdbWinApi.dll" to "api".encodeToByteArray()
        )
        val directory = Files.createTempDirectory("afm-runtime-").toFile()
        try {
            assertFalse(ZipUtils.archiveMatches(ByteArrayInputStream(archive), directory.absolutePath))

            ZipUtils.unzip(ByteArrayInputStream(archive), directory.absolutePath)
            assertTrue(ZipUtils.archiveMatches(ByteArrayInputStream(archive), directory.absolutePath))

            directory.resolve("adb.exe").writeText("changed")
            assertFalse(ZipUtils.archiveMatches(ByteArrayInputStream(archive), directory.absolutePath))
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            ZipOutputStream(bytes).use { zip ->
                entries.forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content)
                    zip.closeEntry()
                }
            }
            bytes.toByteArray()
        }
}
